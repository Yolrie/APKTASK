package com.example.apktask.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apktask.R
import com.example.apktask.databinding.ActivityMainBinding
import com.example.apktask.model.TaskStatus
import com.example.apktask.util.NotificationHelper
import com.example.apktask.util.WorkScheduler
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Activité principale — rôle limité à :
 *  1. Lier les vues au ViewModel (collecte les StateFlow)
 *  2. Transmettre les actions utilisateur au ViewModel
 *  3. Mettre à jour l'interface en réponse aux StateFlow
 *  4. Gérer la navigation entre les onglets Tâches et Historique
 *
 * Sécurité :
 *  - FLAG_SECURE positionné AVANT super.onCreate() pour garantir qu'aucune
 *    frame du contenu ne soit jamais exposée (captures d'écran, switcher,
 *    enregistrement d'écran, Accessibility Services malveillants).
 *
 * Collection StateFlow :
 *  - repeatOnLifecycle(STARTED) : les collectors sont suspendus quand l'activité
 *    passe en arrière-plan (STOPPED) et reprennent à STARTED — évite les mises
 *    à jour UI sur une vue non visible, sans fuite mémoire.
 *
 * Aucune logique métier ne doit résider ici.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()

    private lateinit var adapterEnCours: TaskAdapter
    private lateinit var adapterTerminees: TaskAdapter
    private lateinit var adapterAnnulees: TaskAdapter

    private var historyFragment: HistoryFragment? = null

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* NotificationWorker vérifie la permission au moment de s'exécuter */ }

    // ── Cycle de vie ─────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupRecyclerViews()
        setupClickListeners()
        setupBottomNavigation()
        collectViewModelState()

        NotificationHelper.createChannel(this)
        WorkScheduler.init(this)
        requestNotificationPermission()
    }

    // ── Configuration initiale ────────────────────────────────────────────────

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupRecyclerViews() {
        adapterEnCours = TaskAdapter(
            onStartEdit = { viewModel.startEditing(it) },
            onSaveEdit = { id, title -> viewModel.saveEdit(id, title) },
            onCancelEdit = { viewModel.cancelEditing(it) },
            onDelete = { viewModel.deleteTask(it) },
            onMarkDone = { viewModel.setStatus(it, TaskStatus.COMPLETED) },
            onMarkCancelled = { viewModel.setStatus(it, TaskStatus.CANCELLED) }
        )

        adapterTerminees = TaskAdapter()
        adapterAnnulees = TaskAdapter()

        binding.rvEnCours.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = adapterEnCours
            isNestedScrollingEnabled = false
        }

        binding.rvTerminees.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = adapterTerminees
            isNestedScrollingEnabled = false
        }

        binding.rvAnnulees.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = adapterAnnulees
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnAddTask.setOnClickListener {
            val input = binding.etNewTask.text?.toString().orEmpty()
            viewModel.addTask(input)
            if (viewModel.errorMessage.value == null) {
                binding.etNewTask.text?.clear()
                hideKeyboard()
            }
        }

        binding.btnEnregistrer.setOnClickListener {
            viewModel.registerSession()
            hideKeyboard()
        }

        binding.btnReset.setOnClickListener {
            viewModel.resetAll()
        }
    }

    // ── Navigation inférieure ──────────────────────────────────────────────────

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    showTasksView()
                    true
                }
                R.id.nav_history -> {
                    showHistoryView()
                    true
                }
                else -> false
            }
        }
    }

    private fun showTasksView() {
        binding.layoutTasksContent.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE
        hideKeyboard()
    }

    private fun showHistoryView() {
        binding.layoutTasksContent.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE
        hideKeyboard()

        if (historyFragment == null) {
            historyFragment = HistoryFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, historyFragment!!)
                .commit()
        }
        // HistoryFragment refreshes automatically in onResume()
    }

    // ── Collection des StateFlow ──────────────────────────────────────────────

    private fun collectViewModelState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.tasksUiState.collect { items ->
                        val enCours = items.filter {
                            it.task.status == TaskStatus.DRAFT ||
                                    it.task.status == TaskStatus.IN_PROGRESS
                        }
                        val terminees = items.filter { it.task.status == TaskStatus.COMPLETED }
                        val annulees = items.filter { it.task.status == TaskStatus.CANCELLED }

                        adapterEnCours.submitList(enCours)
                        adapterTerminees.submitList(terminees)
                        adapterAnnulees.submitList(annulees)

                        updateCounters(items)
                        updateProgress(items)
                        updateSectionVisibility(terminees.isNotEmpty(), annulees.isNotEmpty())
                    }
                }

                launch {
                    viewModel.isSessionRegistered.collect { isRegistered ->
                        binding.layoutAddTask.visibility =
                            if (isRegistered) View.GONE else View.VISIBLE
                        binding.btnEnregistrer.visibility =
                            if (isRegistered) View.GONE else View.VISIBLE
                        binding.btnReset.visibility =
                            if (isRegistered) View.VISIBLE else View.GONE

                        binding.tvSectionEnCours.visibility =
                            if (isRegistered) View.VISIBLE else View.GONE
                        binding.tvSectionTerminees.visibility =
                            if (isRegistered) View.VISIBLE else View.GONE
                        binding.tvSectionAnnulees.visibility =
                            if (isRegistered) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.errorMessage.collect { message ->
                        message?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    // ── Mise à jour des vues dérivées ─────────────────────────────────────────

    private fun updateCounters(items: List<TaskUiState>) {
        val enCours = items.count {
            it.task.status == TaskStatus.DRAFT || it.task.status == TaskStatus.IN_PROGRESS
        }
        val terminees = items.count { it.task.status == TaskStatus.COMPLETED }
        val annulees = items.count { it.task.status == TaskStatus.CANCELLED }

        binding.tvCounterEnCours.text = getString(R.string.counter_en_cours, enCours)
        binding.tvCounterTerminees.text = getString(R.string.counter_terminees, terminees)
        binding.tvCounterAnnulees.text = getString(R.string.counter_annulees, annulees)
    }

    private fun updateProgress(items: List<TaskUiState>) {
        val total = items.size
        val done = items.count { it.task.status == TaskStatus.COMPLETED }
        val progress = if (total > 0) (done * 100) / total else 0
        binding.progressBar.progress = progress
    }

    private fun updateSectionVisibility(hasTerminees: Boolean, hasAnnulees: Boolean) {
        val isRegistered = viewModel.isSessionRegistered.value
        binding.tvSectionTerminees.visibility =
            if (isRegistered && hasTerminees) View.VISIBLE else View.GONE
        binding.tvSectionAnnulees.visibility =
            if (isRegistered && hasAnnulees) View.VISIBLE else View.GONE
        binding.rvTerminees.visibility =
            if (isRegistered && hasTerminees) View.VISIBLE else View.GONE
        binding.rvAnnulees.visibility =
            if (isRegistered && hasAnnulees) View.VISIBLE else View.GONE
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}

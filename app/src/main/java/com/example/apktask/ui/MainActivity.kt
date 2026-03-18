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

    // Trois adapters pour les trois zones, partageant la même classe.
    private lateinit var adapterEnCours: TaskAdapter
    private lateinit var adapterTerminees: TaskAdapter
    private lateinit var adapterAnnulees: TaskAdapter

    /**
     * Launcher pour la demande de permission POST_NOTIFICATIONS (Android 13+).
     * Déclaré ici (avant onCreate) : registerForActivityResult doit être appelé
     * avant que l'activité atteigne STARTED.
     * Aucune action sur refus : NotificationWorker vérifie la permission à l'exécution.
     */
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* accordée ou refusée : NotificationWorker vérifie la permission au moment de s'exécuter */ }

    // ── Cycle de vie ─────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // ⚠️ FLAG_SECURE doit impérativement précéder super.onCreate().
        // La surface window est allouée dans Activity.attach(), avant onCreate().
        // Placé ici, aucune frame n'est jamais rendue sans le flag actif.
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
        collectViewModelState()

        // Canal de notification créé avant tout Worker (idempotent)
        NotificationHelper.createChannel(this)
        // Planification WorkManager (idempotent — UniquePeriodicWork KEEP/REPLACE)
        WorkScheduler.init(this)
        // Demande POST_NOTIFICATIONS si nécessaire (Android 13+)
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
        // Adapter pour les tâches actives — callbacks complets
        adapterEnCours = TaskAdapter(
            onStartEdit = { viewModel.startEditing(it) },
            onSaveEdit = { id, title -> viewModel.saveEdit(id, title) },
            onCancelEdit = { viewModel.cancelEditing(it) },
            onDelete = { viewModel.deleteTask(it) },
            onMarkDone = { viewModel.setStatus(it, TaskStatus.COMPLETED) },
            onMarkCancelled = { viewModel.setStatus(it, TaskStatus.CANCELLED) }
        )

        // Adapters lecture seule pour les zones terminées et annulées
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
            // Le champ se vide seulement si l'ajout a réussi (pas d'erreur active)
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

    // ── Collection des StateFlow ──────────────────────────────────────────────

    /**
     * Collecte les StateFlow du ViewModel avec [repeatOnLifecycle].
     *
     * repeatOnLifecycle(STARTED) :
     *  - Lance les collectors à onStart(), les suspend à onStop()
     *  - Garantit que l'UI n'est jamais mise à jour en arrière-plan
     *  - Pas de fuite mémoire : les jobs sont annulés à onDestroy()
     *  - Les trois [launch] s'exécutent en parallèle dans le même bloc repeat
     */
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
                        // Zone d'ajout de tâche : cachée après enregistrement
                        binding.layoutAddTask.visibility =
                            if (isRegistered) View.GONE else View.VISIBLE
                        binding.btnEnregistrer.visibility =
                            if (isRegistered) View.GONE else View.VISIBLE
                        binding.btnReset.visibility =
                            if (isRegistered) View.VISIBLE else View.GONE

                        // Titres de sections : visibles uniquement après enregistrement
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

    /**
     * Demande POST_NOTIFICATIONS sur Android 13+ si la permission n'est pas encore accordée.
     * Sur les versions antérieures, les notifications ne nécessitent pas de permission runtime.
     */
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

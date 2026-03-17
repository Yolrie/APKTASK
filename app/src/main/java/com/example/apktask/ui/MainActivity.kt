package com.example.apktask.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apktask.R
import com.example.apktask.databinding.ActivityMainBinding
import com.example.apktask.model.TaskStatus
import com.google.android.material.snackbar.Snackbar

/**
 * Activité principale — rôle limité à :
 *  1. Lier les vues au ViewModel (observe LiveData)
 *  2. Transmettre les actions utilisateur au ViewModel
 *  3. Mettre à jour l'interface en réponse aux LiveData
 *
 * Sécurité :
 *  - FLAG_SECURE positionné AVANT setContentView() pour garantir qu'aucune
 *    frame du contenu ne soit jamais exposée (captures d'écran, switcher,
 *    enregistrement d'écran, Accessibility Services malveillants).
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

    // ── Cycle de vie ─────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // ⚠️ FLAG_SECURE doit impérativement précéder setContentView().
        // Si posé après, Android peut capturer une première frame non protégée
        // (visible dans le switcher d'apps ou via screenshot racing).
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
        observeViewModel()
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
            // Le champ se vide seulement si l'ajout a réussi (pas d'erreur observée)
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

    // ── Observation du ViewModel ──────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.tasksUiState.observe(this) { items ->
            val enCours = items.filter {
                it.task.status == TaskStatus.DRAFT || it.task.status == TaskStatus.IN_PROGRESS
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

        viewModel.isSessionRegistered.observe(this) { isRegistered ->
            // Zone d'ajout de tâche : cachée après enregistrement
            binding.layoutAddTask.visibility = if (isRegistered) View.GONE else View.VISIBLE
            binding.btnEnregistrer.visibility = if (isRegistered) View.GONE else View.VISIBLE
            binding.btnReset.visibility = if (isRegistered) View.VISIBLE else View.GONE

            // Titres de sections : visibles uniquement après enregistrement
            binding.tvSectionEnCours.visibility = if (isRegistered) View.VISIBLE else View.GONE
            binding.tvSectionTerminees.visibility = if (isRegistered) View.VISIBLE else View.GONE
            binding.tvSectionAnnulees.visibility = if (isRegistered) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearError()
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
        val isRegistered = viewModel.isSessionRegistered.value == true
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

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}

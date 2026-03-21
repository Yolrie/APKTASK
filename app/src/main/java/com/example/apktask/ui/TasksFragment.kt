package com.example.apktask.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apktask.R
import com.example.apktask.databinding.FragmentTasksBinding
import com.example.apktask.model.TaskStatus
import com.example.apktask.ui.swipe.SwipeActionCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Fragment for the Tasks tab.
 *
 * Collects StateFlow from TaskViewModel using repeatOnLifecycle(STARTED):
 *  - Collectors are suspended when the fragment is not visible (onStop)
 *  - Resumed at onStart — no background UI updates, no memory leaks.
 */
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TaskViewModel by viewModels()

    private lateinit var adapterEnCours: TaskAdapter
    private lateinit var adapterTerminees: TaskAdapter
    private lateinit var adapterAnnulees: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader()
        setupRecyclerViews()
        setupClickListeners()
        collectViewModelState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    private fun setupHeader() {
        binding.tvDate.text = viewModel.todayReadable
        binding.tvMotivation.text = viewModel.motivationalMessage
    }

    private fun setupRecyclerViews() {
        adapterEnCours = TaskAdapter(
            onStartEdit = { viewModel.startEditing(it) },
            onSaveEdit = { id, title -> viewModel.saveEdit(id, title) },
            onCancelEdit = { viewModel.cancelEditing(it) },
            onDelete = { viewModel.deleteTask(it) },
            onMarkDone = { viewModel.setStatus(it, TaskStatus.COMPLETED) },
            onMarkCancelled = { viewModel.setStatus(it, TaskStatus.CANCELLED) },
            onCyclePriority = { viewModel.cyclePriority(it) }
        )
        adapterTerminees = TaskAdapter()
        adapterAnnulees = TaskAdapter()

        binding.rvEnCours.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterEnCours
            isNestedScrollingEnabled = false
        }
        attachSwipeGestures()
        binding.rvTerminees.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = adapterTerminees
            isNestedScrollingEnabled = false
        }
        binding.rvAnnulees.apply {
            layoutManager = LinearLayoutManager(requireContext())
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

    // ── Swipe gestures ────────────────────────────────────────────────────────

    private fun attachSwipeGestures() {
        val callback = SwipeActionCallback(
            context = requireContext(),
            getItem = { position -> adapterEnCours.currentList[position] },
            onMarkDone = { taskId -> viewModel.setStatus(taskId, TaskStatus.COMPLETED) },
            onDelete = { taskId -> viewModel.deleteTask(taskId) },
            onCancel = { taskId -> viewModel.setStatus(taskId, TaskStatus.CANCELLED) }
        )
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvEnCours)
    }

    // ── StateFlow collection ──────────────────────────────────────────────────

    private fun collectViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        // Empty state: visible only when not registered and no tasks at all
                        val isEmpty = items.isEmpty() && !viewModel.isSessionRegistered.value
                        binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
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
                    }
                }

                launch {
                    viewModel.streak.collect { streak ->
                        if (streak.count > 0) {
                            binding.tvStreak.visibility = View.VISIBLE
                            binding.tvStreak.text =
                                getString(R.string.streak_label, streak.count, streak.badge)
                        } else {
                            binding.tvStreak.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { msg ->
                        msg?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    // ── Derived view updates ──────────────────────────────────────────────────

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
        binding.progressBar.progress = if (total > 0) (done * 100) / total else 0
    }

    private fun updateSectionVisibility(hasTerminees: Boolean, hasAnnulees: Boolean) {
        val isRegistered = viewModel.isSessionRegistered.value
        binding.tvSectionTerminees.visibility =
            if (isRegistered && hasTerminees) View.VISIBLE else View.GONE
        binding.rvTerminees.visibility =
            if (isRegistered && hasTerminees) View.VISIBLE else View.GONE
        binding.tvSectionAnnulees.visibility =
            if (isRegistered && hasAnnulees) View.VISIBLE else View.GONE
        binding.rvAnnulees.visibility =
            if (isRegistered && hasAnnulees) View.VISIBLE else View.GONE
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        view?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }
}

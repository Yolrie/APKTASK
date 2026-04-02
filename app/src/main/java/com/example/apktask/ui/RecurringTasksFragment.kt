package com.example.apktask.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apktask.R
import com.example.apktask.databinding.FragmentRecurringTasksBinding
import com.example.apktask.model.Frequency
import com.example.apktask.model.Priority
import com.example.apktask.model.RecurrenceRule
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

class RecurringTasksFragment : Fragment() {

    private var _binding: FragmentRecurringTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecurringTaskViewModel by viewModels()

    private lateinit var adapter: RecurringTaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecurringTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RecurringTaskAdapter(
            onToggleActive = { viewModel.toggleActive(it) },
            onDelete = { viewModel.delete(it) }
        )

        binding.rvRoutines.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecurringTasksFragment.adapter
            isNestedScrollingEnabled = false
        }

        binding.fabAddRoutine.setOnClickListener { showAddDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.routines.collect { list ->
                    adapter.submitList(list)
                    binding.layoutEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvRoutines.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Add dialog ────────────────────────────────────────────────────────────

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_routine, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etRoutineTitle)
        val chipGroupFreq = dialogView.findViewById<ChipGroup>(R.id.chipGroupFrequency)
        val chipGroupDays = dialogView.findViewById<ChipGroup>(R.id.chipGroupDays)
        val chipGroupPriority = dialogView.findViewById<ChipGroup>(R.id.chipGroupPriority)

        // Show/hide days section based on frequency
        chipGroupFreq.setOnCheckedStateChangeListener { group, _ ->
            val checkedId = group.checkedChipId
            val freq = frequencyFromChipId(checkedId)
            chipGroupDays.visibility =
                if (freq == Frequency.WEEKLY || freq == Frequency.CUSTOM) View.VISIBLE else View.GONE
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_routine_title)
            .setView(dialogView)
            .setPositiveButton(R.string.add_routine_confirm) { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isBlank()) return@setPositiveButton

                val freq = frequencyFromChipId(chipGroupFreq.checkedChipId)
                val priority = priorityFromChipId(chipGroupPriority.checkedChipId)
                val rule = buildRule(freq, chipGroupDays)

                viewModel.addRoutine(title, rule, priority)
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun frequencyFromChipId(id: Int): Frequency = when (id) {
        R.id.chipFreqDaily -> Frequency.DAILY
        R.id.chipFreqWeekdays -> Frequency.WEEKDAYS
        R.id.chipFreqWeekly -> Frequency.WEEKLY
        R.id.chipFreqCustom -> Frequency.CUSTOM
        else -> Frequency.DAILY
    }

    private fun priorityFromChipId(id: Int): Priority = when (id) {
        R.id.chipPriorityLow -> Priority.LOW
        R.id.chipPriorityMedium -> Priority.MEDIUM
        R.id.chipPriorityHigh -> Priority.HIGH
        else -> Priority.NONE
    }

    private fun buildRule(freq: Frequency, chipGroupDays: ChipGroup): RecurrenceRule {
        return when (freq) {
            Frequency.DAILY -> RecurrenceRule.daily()
            Frequency.WEEKDAYS -> RecurrenceRule.weekdays()
            Frequency.WEEKLY, Frequency.CUSTOM -> {
                val days = mutableListOf<Int>()
                for (i in 0 until chipGroupDays.childCount) {
                    val chip = chipGroupDays.getChildAt(i) as? Chip ?: continue
                    if (chip.isChecked) {
                        val day = dayFromChipId(chip.id)
                        if (day != null) days.add(day)
                    }
                }
                if (days.isEmpty()) RecurrenceRule.daily()
                else if (freq == Frequency.WEEKLY) RecurrenceRule.weekly(days.first())
                else RecurrenceRule.custom(*days.toIntArray())
            }
        }
    }

    private fun dayFromChipId(id: Int): Int? = when (id) {
        R.id.chipDayMon -> Calendar.MONDAY
        R.id.chipDayTue -> Calendar.TUESDAY
        R.id.chipDayWed -> Calendar.WEDNESDAY
        R.id.chipDayThu -> Calendar.THURSDAY
        R.id.chipDayFri -> Calendar.FRIDAY
        R.id.chipDaySat -> Calendar.SATURDAY
        R.id.chipDaySun -> Calendar.SUNDAY
        else -> null
    }
}

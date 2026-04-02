package com.example.apktask.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apktask.databinding.ItemRecurringTaskBinding
import com.example.apktask.model.Frequency
import com.example.apktask.model.Priority
import com.example.apktask.model.RecurringTask
import java.util.Calendar

class RecurringTaskAdapter(
    private val onToggleActive: (RecurringTask) -> Unit,
    private val onDelete: (RecurringTask) -> Unit
) : ListAdapter<RecurringTask, RecurringTaskAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecurringTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val b: ItemRecurringTaskBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(routine: RecurringTask) {
            b.tvRoutineTitle.text = routine.title
            b.tvRoutineRule.text = ruleLabel(routine)
            b.tvRoutinePriority.text = priorityLabel(routine.priority)

            // Prevent the listener from firing during bind
            b.switchActive.setOnCheckedChangeListener(null)
            b.switchActive.isChecked = routine.isActive
            b.switchActive.setOnCheckedChangeListener { _, _ -> onToggleActive(routine) }

            b.btnDelete.setOnClickListener { onDelete(routine) }
        }

        private fun ruleLabel(routine: RecurringTask): String {
            val rule = routine.rule
            return when (rule.frequency) {
                Frequency.DAILY -> "Chaque jour"
                Frequency.WEEKDAYS -> "Lun – Ven"
                Frequency.WEEKLY, Frequency.CUSTOM -> buildDayList(rule.daysBitmask)
            }
        }

        private fun buildDayList(bitmask: Int): String {
            val dayNames = mapOf(
                Calendar.MONDAY to "Lun",
                Calendar.TUESDAY to "Mar",
                Calendar.WEDNESDAY to "Mer",
                Calendar.THURSDAY to "Jeu",
                Calendar.FRIDAY to "Ven",
                Calendar.SATURDAY to "Sam",
                Calendar.SUNDAY to "Dim"
            )
            return dayNames.entries
                .filter { (day, _) -> bitmask and (1 shl day) != 0 }
                .joinToString(", ") { it.value }
                .ifEmpty { "—" }
        }

        private fun priorityLabel(priority: Priority): String = when (priority) {
            Priority.NONE -> ""
            Priority.LOW -> "🔵 Basse"
            Priority.MEDIUM -> "🟡 Moyenne"
            Priority.HIGH -> "🔴 Haute"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RecurringTask>() {
            override fun areItemsTheSame(a: RecurringTask, b: RecurringTask) = a.id == b.id
            override fun areContentsTheSame(a: RecurringTask, b: RecurringTask) = a == b
        }
    }
}

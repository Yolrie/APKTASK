package com.example.apktask.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apktask.R
import com.example.apktask.databinding.ItemTaskActiveBinding
import com.example.apktask.databinding.ItemTaskDoneBinding
import com.example.apktask.model.Priority
import com.example.apktask.model.TaskStatus

/**
 * Adapter RecyclerView gérant 4 états de tâche via 2 types de vues :
 *
 *  TYPE_ACTIVE — tâches DRAFT ou IN_PROGRESS : actions + chip de priorité
 *  TYPE_DONE   — tâches COMPLETED ou CANCELLED : lecture seule, colorées
 *
 * Les callbacks sont nommés explicitement pour que leur usage soit clair
 * côté appelant et qu'aucune action involontaire ne soit déclenchée.
 */
class TaskAdapter(
    private val onStartEdit: (taskId: Int) -> Unit = {},
    private val onSaveEdit: (taskId: Int, newTitle: String) -> Unit = { _, _ -> },
    private val onCancelEdit: (taskId: Int) -> Unit = {},
    private val onDelete: (taskId: Int) -> Unit = {},
    private val onMarkDone: (taskId: Int) -> Unit = {},
    private val onMarkCancelled: (taskId: Int) -> Unit = {},
    private val onCyclePriority: (taskId: Int) -> Unit = {}
) : ListAdapter<TaskUiState, RecyclerView.ViewHolder>(Diff()) {

    init {
        // Stable IDs allow RecyclerView to animate insertions/deletions/moves
        // correctly when items are added (undo), removed (delete), or reordered (priority).
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).task.id.toLong()

    // ── Types de vues ────────────────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int =
        when (getItem(position).task.status) {
            TaskStatus.COMPLETED, TaskStatus.CANCELLED -> TYPE_DONE
            else -> TYPE_ACTIVE
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ACTIVE -> ActiveHolder(ItemTaskActiveBinding.inflate(inflater, parent, false))
            else -> DoneHolder(ItemTaskDoneBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ActiveHolder -> holder.bind(item)
            is DoneHolder -> holder.bind(item)
        }
    }

    // ── ViewHolder : tâche active (DRAFT / IN_PROGRESS) ─────────────────────

    inner class ActiveHolder(private val b: ItemTaskActiveBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: TaskUiState) {
            val task = item.task
            b.etTaskTitle.setText(task.title)

            bindPriorityChip(task.id, task.priority)

            when {
                task.status == TaskStatus.DRAFT && item.isEditing -> bindDraftEditing(task.id)
                task.status == TaskStatus.DRAFT -> bindDraftViewing(task.id)
                task.status == TaskStatus.IN_PROGRESS -> bindInProgress(task.id)
                else -> Unit
            }
        }

        /**
         * Binds the priority chip: shows label + emoji, colors the left accent bar,
         * and sets up the tap-to-cycle listener.
         */
        private fun bindPriorityChip(taskId: Int, priority: Priority) {
            b.chipPriority.text = when (priority) {
                Priority.NONE -> b.root.context.getString(R.string.priority_none)
                Priority.LOW -> b.root.context.getString(R.string.priority_low)
                Priority.MEDIUM -> b.root.context.getString(R.string.priority_medium)
                Priority.HIGH -> b.root.context.getString(R.string.priority_high)
            }

            val barColor = priorityColor(priority)
            b.viewPriorityBar.setBackgroundColor(
                ContextCompat.getColor(b.root.context, barColor)
            )

            b.chipPriority.setOnClickListener { onCyclePriority(taskId) }
        }

        /** Mode lecture seule avant édition : boutons Modifier / Supprimer. */
        private fun bindDraftViewing(taskId: Int) = b.apply {
            etTaskTitle.isEnabled = false
            etTaskTitle.setTextColor(
                ContextCompat.getColor(root.context, R.color.text_primary)
            )
            btnAction.setText(R.string.button_edit)
            btnSecondary.setText(R.string.button_delete)
            btnSecondary.visibility = View.VISIBLE

            btnAction.setOnClickListener { onStartEdit(taskId) }
            btnSecondary.setOnClickListener { onDelete(taskId) }
        }

        /** Mode édition : champ actif, boutons Valider / Annuler. */
        private fun bindDraftEditing(taskId: Int) = b.apply {
            etTaskTitle.isEnabled = true
            etTaskTitle.requestFocus()
            btnAction.setText(R.string.button_validate)
            btnSecondary.setText(R.string.button_cancel)
            btnSecondary.visibility = View.VISIBLE

            btnAction.setOnClickListener {
                onSaveEdit(taskId, etTaskTitle.text.toString())
            }
            btnSecondary.setOnClickListener { onCancelEdit(taskId) }
        }

        /** Session enregistrée : boutons Terminé / Annuler. */
        private fun bindInProgress(taskId: Int) = b.apply {
            etTaskTitle.isEnabled = false
            etTaskTitle.setTextColor(
                ContextCompat.getColor(root.context, R.color.text_primary)
            )
            btnAction.setText(R.string.button_done)
            btnSecondary.setText(R.string.button_cancel)
            btnSecondary.visibility = View.VISIBLE

            btnAction.setOnClickListener { onMarkDone(taskId) }
            btnSecondary.setOnClickListener { onMarkCancelled(taskId) }
        }

        private fun priorityColor(priority: Priority): Int = when (priority) {
            Priority.HIGH -> R.color.error
            Priority.MEDIUM -> R.color.accent
            Priority.LOW -> R.color.primary
            Priority.NONE -> R.color.border
        }
    }

    // ── ViewHolder : tâche terminée / annulée ────────────────────────────────

    inner class DoneHolder(private val b: ItemTaskDoneBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: TaskUiState) {
            b.tvTaskTitle.text = item.task.title
            val colorRes = when (item.task.status) {
                TaskStatus.COMPLETED -> R.color.success
                TaskStatus.CANCELLED -> R.color.error
                else -> R.color.text_primary
            }
            val color = ContextCompat.getColor(b.root.context, colorRes)
            b.tvTaskTitle.setTextColor(color)
            b.viewStatusBar.setBackgroundColor(color)
        }
    }

    // ── DiffUtil ─────────────────────────────────────────────────────────────

    private class Diff : DiffUtil.ItemCallback<TaskUiState>() {
        override fun areItemsTheSame(old: TaskUiState, new: TaskUiState): Boolean =
            old.task.id == new.task.id

        override fun areContentsTheSame(old: TaskUiState, new: TaskUiState): Boolean =
            old == new
    }

    companion object {
        private const val TYPE_ACTIVE = 0
        private const val TYPE_DONE = 1
    }
}

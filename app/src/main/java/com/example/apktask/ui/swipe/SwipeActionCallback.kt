package com.example.apktask.ui.swipe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.apktask.R
import com.example.apktask.model.TaskStatus
import com.example.apktask.ui.TaskAdapter
import com.example.apktask.ui.TaskUiState
import kotlin.math.abs
import kotlin.math.min

/**
 * Swipe gesture handler for the active-tasks RecyclerView.
 *
 * Gestures:
 *  - Swipe RIGHT → mark task COMPLETED (green overlay + ✓ icon)
 *  - Swipe LEFT  → DRAFT: delete task (red overlay + 🗑 icon)
 *                  IN_PROGRESS: cancel task (red overlay + 🗑 icon)
 *
 * Only [TaskAdapter.TYPE_ACTIVE] items (position TYPE_DONE = 1) are swipeable —
 * completed/cancelled rows are blocked via [getSwipeDirs].
 *
 * [onSwiped] delegates to the provided callbacks; the adapter will be notified
 * via ListAdapter.submitList when the ViewModel updates.
 */
class SwipeActionCallback(
    context: Context,
    private val getItem: (adapterPosition: Int) -> TaskUiState,
    private val onMarkDone: (taskId: Int) -> Unit,
    private val onDelete: (taskId: Int) -> Unit,
    private val onCancel: (taskId: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(
    0, // no drag
    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
) {

    private val colorGreen = ContextCompat.getColor(context, R.color.success)
    private val colorRed = ContextCompat.getColor(context, R.color.error)
    private val iconDone = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)!!
    private val iconDelete = ContextCompat.getDrawable(context, R.drawable.ic_delete_swipe)!!
    private val cornerRadius = context.resources.getDimension(R.dimen.card_corner_radius)
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_size)
    private val iconMargin = context.resources.getDimensionPixelSize(R.dimen.swipe_icon_margin)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── Swipe eligibility ─────────────────────────────────────────────────────

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val item = runCatching { getItem(viewHolder.bindingAdapterPosition) }.getOrNull()
            ?: return 0
        // Only swipe active (DRAFT / IN_PROGRESS) items
        return if (item.task.status == TaskStatus.DRAFT ||
            item.task.status == TaskStatus.IN_PROGRESS
        ) {
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else {
            0
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false // drag-and-drop disabled

    // ── Action dispatch ───────────────────────────────────────────────────────

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val item = runCatching { getItem(viewHolder.bindingAdapterPosition) }.getOrNull()
            ?: return
        val task = item.task
        when (direction) {
            ItemTouchHelper.RIGHT -> onMarkDone(task.id)
            ItemTouchHelper.LEFT -> when (task.status) {
                TaskStatus.IN_PROGRESS -> onCancel(task.id)
                else -> onDelete(task.id)
            }
        }
    }

    // ── Visual feedback ───────────────────────────────────────────────────────

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        val itemView = viewHolder.itemView
        val swipeRight = dX > 0

        // Clamp alpha: full opacity at half-item swipe
        val alpha = min(1f, abs(dX) / (itemView.width * 0.5f))

        // Background color
        bgPaint.color = if (swipeRight) colorGreen else colorRed
        bgPaint.alpha = (alpha * 255).toInt()

        // Rounded rect background (clip to card corners)
        val bgRect = if (swipeRight) {
            RectF(
                itemView.left.toFloat(),
                itemView.top.toFloat(),
                itemView.left + dX,
                itemView.bottom.toFloat()
            )
        } else {
            RectF(
                itemView.right + dX,
                itemView.top.toFloat(),
                itemView.right.toFloat(),
                itemView.bottom.toFloat()
            )
        }
        c.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)

        // Icon
        val icon = if (swipeRight) iconDone else iconDelete
        icon.colorFilter = PorterDuffColorFilter(
            0xFFFFFFFF.toInt(),
            PorterDuff.Mode.SRC_IN
        )
        icon.alpha = (alpha * 255).toInt()

        val iconTop = itemView.top + (itemView.height - iconSize) / 2
        if (swipeRight) {
            icon.setBounds(
                itemView.left + iconMargin,
                iconTop,
                itemView.left + iconMargin + iconSize,
                iconTop + iconSize
            )
        } else {
            icon.setBounds(
                itemView.right - iconMargin - iconSize,
                iconTop,
                itemView.right - iconMargin,
                iconTop + iconSize
            )
        }
        icon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}

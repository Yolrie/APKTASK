package com.example.apktask.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apktask.R
import com.example.apktask.databinding.ItemDaySummaryBinding
import com.example.apktask.model.DaySummary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter pour la liste des bilans journaliers dans l'onglet Historique.
 *
 * Chaque item affiche :
 *  - Un emoji de performance (parfait, bon, moyen, faible)
 *  - La date lisible en français
 *  - Les compteurs (terminées, annulées, total)
 *  - Une barre de progression colorée
 *  - Le pourcentage d'accomplissement
 */
class DaySummaryAdapter : ListAdapter<DaySummary, DaySummaryAdapter.SummaryViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val binding = ItemDaySummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SummaryViewHolder(private val b: ItemDaySummaryBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(summary: DaySummary) {
            val ctx = b.root.context

            b.tvEmoji.text = summary.performanceEmoji
            b.tvDate.text = formatDateReadable(summary.date)

            b.tvCompleted.text = ctx.getString(
                R.string.history_completed_count, summary.completedTasks
            )
            b.tvCancelled.text = if (summary.cancelledTasks > 0) {
                ctx.getString(R.string.history_cancelled_count, summary.cancelledTasks)
            } else ""
            b.tvTotal.text = ctx.getString(
                R.string.history_total_count, summary.totalTasks
            )

            b.progressDay.progress = summary.completionPercent
            val progressColor = when {
                summary.allDone -> ContextCompat.getColor(ctx, R.color.success)
                summary.completionPercent >= 50 -> ContextCompat.getColor(ctx, R.color.accent)
                else -> ContextCompat.getColor(ctx, R.color.text_secondary)
            }
            b.progressDay.setIndicatorColor(progressColor)

            b.tvPercent.text = ctx.getString(R.string.history_percent, summary.completionPercent)
            b.tvPercent.setTextColor(
                when {
                    summary.allDone -> ContextCompat.getColor(ctx, R.color.success)
                    summary.completionPercent >= 50 -> ContextCompat.getColor(ctx, R.color.accent)
                    else -> ContextCompat.getColor(ctx, R.color.text_secondary)
                }
            )

            if (summary.streakAtDay > 0) {
                b.tvStreakBadge.visibility = View.VISIBLE
                b.tvStreakBadge.text = ctx.getString(R.string.history_streak_days, summary.streakAtDay)
            } else {
                b.tvStreakBadge.visibility = View.GONE
            }
        }

        private fun formatDateReadable(isoDate: String): String {
            return try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val readableFormat = SimpleDateFormat("EEEE dd MMM", Locale.FRENCH)
                val date = isoFormat.parse(isoDate) ?: return isoDate
                readableFormat.format(date).replaceFirstChar { it.uppercaseChar() }
            } catch (_: Exception) {
                isoDate
            }
        }
    }

    private class Diff : DiffUtil.ItemCallback<DaySummary>() {
        override fun areItemsTheSame(old: DaySummary, new: DaySummary): Boolean =
            old.date == new.date

        override fun areContentsTheSame(old: DaySummary, new: DaySummary): Boolean =
            old == new
    }
}

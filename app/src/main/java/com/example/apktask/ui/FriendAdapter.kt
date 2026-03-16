package com.example.apktask.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apktask.R
import com.example.apktask.databinding.ItemFriendProgressBinding
import com.example.apktask.model.FriendProgress

/**
 * Adapter pour la liste des amis dans l'onglet Social.
 * Affiche la progression journalière de chaque ami.
 */
class FriendAdapter(
    private val onRemoveFriend: (userId: String) -> Unit = {}
) : ListAdapter<FriendProgress, FriendAdapter.FriendViewHolder>(Diff()) {

    private val avatarColors = listOf(
        R.color.avatar_0, R.color.avatar_1, R.color.avatar_2, R.color.avatar_3,
        R.color.avatar_4, R.color.avatar_5, R.color.avatar_6, R.color.avatar_7
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendProgressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(private val b: ItemFriendProgressBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(friend: FriendProgress) {
            // Avatar
            val colorRes = avatarColors.getOrElse(friend.avatarColorIndex) { R.color.avatar_0 }
            b.viewAvatarBg.setBackgroundColor(ContextCompat.getColor(b.root.context, colorRes))
            b.tvAvatarLetter.text = friend.avatarLetter

            // Nom
            b.tvFriendName.text = friend.displayName

            // Série
            if (friend.streak > 0) {
                b.tvStreak.visibility = View.VISIBLE
                b.tvStreak.text = b.root.context.getString(R.string.streak_days, friend.streak)
            } else {
                b.tvStreak.visibility = View.GONE
            }

            // Progression
            b.tvProgress.text = b.root.context.getString(
                R.string.friend_progress,
                friend.todayCompleted,
                friend.todayTotal
            )
            b.progressFriend.progress = friend.completionPercent

            // Couleur de la barre selon complétion
            val progressColor = when {
                friend.isAllDone -> ContextCompat.getColor(b.root.context, R.color.success)
                friend.completionPercent >= 50 -> ContextCompat.getColor(b.root.context, R.color.accent)
                else -> ContextCompat.getColor(b.root.context, R.color.text_secondary)
            }
            b.progressFriend.setIndicatorColor(progressColor)

            // Badge Démo
            b.chipDemo.visibility = if (friend.isMock) View.VISIBLE else View.GONE

            // Suppression
            b.root.setOnLongClickListener {
                if (!friend.isMock) {
                    onRemoveFriend(friend.userId)
                    true
                } else false
            }
        }
    }

    private class Diff : DiffUtil.ItemCallback<FriendProgress>() {
        override fun areItemsTheSame(old: FriendProgress, new: FriendProgress): Boolean =
            old.userId == new.userId

        override fun areContentsTheSame(old: FriendProgress, new: FriendProgress): Boolean =
            old == new
    }
}

package com.example.apktask.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.apktask.R
import com.example.apktask.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Fragment for the Profile tab.
 *
 * Collects StateFlow from ProfileViewModel using repeatOnLifecycle(STARTED).
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAvatarColorPicker()
        setupClickListeners()
        collectViewModelState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Avatar color picker ───────────────────────────────────────────────────

    private val avatarButtons by lazy {
        listOf(
            binding.avatarColor0, binding.avatarColor1, binding.avatarColor2,
            binding.avatarColor3, binding.avatarColor4, binding.avatarColor5,
            binding.avatarColor6, binding.avatarColor7
        )
    }

    private fun setupAvatarColorPicker() {
        avatarButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.updateAvatarColor(index) }
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnSaveName.setOnClickListener {
            viewModel.updateDisplayName(binding.etDisplayName.text.toString())
        }

        binding.btnCopyCode.setOnClickListener {
            viewModel.copyFriendCode()
        }

        binding.switchPublic.setOnCheckedChangeListener { _, _ ->
            viewModel.togglePublic()
        }

        binding.btnSaveNotifications.setOnClickListener {
            viewModel.updateNotifications(
                morningEnabled = binding.switchMorning.isChecked,
                eveningEnabled = binding.switchEvening.isChecked,
                morningHour = binding.sliderMorning.value.toInt(),
                eveningHour = binding.sliderEvening.value.toInt()
            )
        }

        binding.sliderMorning.addOnChangeListener { _, value, _ ->
            binding.tvMorningHour.text = getString(R.string.hour_format, value.toInt())
        }

        binding.sliderEvening.addOnChangeListener { _, value, _ ->
            binding.tvEveningHour.text = getString(R.string.hour_format, value.toInt())
        }

        binding.btnShare.setOnClickListener { shareProgress() }
    }

    // ── StateFlow collection ──────────────────────────────────────────────────

    private fun collectViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.profile.collect { profile ->
                        // Avatar background color
                        val colorRes = avatarColorRes(profile.avatarColorIndex)
                        binding.viewAvatarBg.setBackgroundColor(
                            requireContext().getColor(colorRes)
                        )
                        binding.tvAvatarLetter.text = profile.avatarLetter

                        // Color picker selection opacity
                        avatarButtons.forEachIndexed { i, btn ->
                            btn.alpha = if (i == profile.avatarColorIndex) 1f else 0.35f
                        }

                        // Name (only pre-fill if field is empty to avoid overwriting user input)
                        if (binding.etDisplayName.text.isNullOrEmpty()) {
                            binding.etDisplayName.setText(profile.displayName)
                        }

                        // Friend code
                        binding.tvFriendCode.text = profile.friendCode

                        // Visibility
                        binding.switchPublic.isChecked = profile.isPublic
                        binding.tvPublicHint.text = if (profile.isPublic)
                            getString(R.string.profile_public_hint_on)
                        else
                            getString(R.string.profile_public_hint_off)

                        // Notification settings
                        binding.switchMorning.isChecked = profile.notifMorningEnabled
                        binding.switchEvening.isChecked = profile.notifEveningEnabled
                        binding.sliderMorning.value = profile.notifMorningHour.toFloat()
                        binding.sliderEvening.value = profile.notifEveningHour.toFloat()
                        binding.tvMorningHour.text =
                            getString(R.string.hour_format, profile.notifMorningHour)
                        binding.tvEveningHour.text =
                            getString(R.string.hour_format, profile.notifEveningHour)
                    }
                }

                launch {
                    viewModel.streak.collect { streak ->
                        binding.tvStreakValue.text = streak.count.toString()
                        binding.tvStreakBadge.text = streak.badge
                        binding.tvStreakRecord.text =
                            getString(R.string.streak_record, streak.longestEver)
                    }
                }

                launch {
                    viewModel.successMessage.collect { msg ->
                        msg?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                            viewModel.clearMessages()
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { msg ->
                        msg?.let {
                            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                            viewModel.clearMessages()
                        }
                    }
                }
            }
        }
    }

    // ── Share ─────────────────────────────────────────────────────────────────

    private fun shareProgress() {
        val text = viewModel.buildShareText(
            completedToday = 0, // TODO: inject from shared TaskViewModel
            totalToday = 0
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_progress_title)))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun avatarColorRes(index: Int): Int = when (index) {
        0 -> R.color.avatar_0
        1 -> R.color.avatar_1
        2 -> R.color.avatar_2
        3 -> R.color.avatar_3
        4 -> R.color.avatar_4
        5 -> R.color.avatar_5
        6 -> R.color.avatar_6
        7 -> R.color.avatar_7
        else -> R.color.avatar_0
    }
}

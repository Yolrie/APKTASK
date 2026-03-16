package com.example.apktask.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.apktask.R
import com.example.apktask.databinding.FragmentProfileBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment de l'onglet Profil.
 *
 * Permet de :
 *  - Modifier son nom et choisir sa couleur d'avatar
 *  - Voir et copier son code ami
 *  - Configurer les rappels (heure matin/soir, activé/désactivé)
 *  - Activer/désactiver la visibilité publique
 *  - Partager sa progression du jour
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
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Couleurs d'avatar ─────────────────────────────────────────────────────

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

    // ── Listeners ─────────────────────────────────────────────────────────────

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

        binding.btnShare.setOnClickListener {
            shareProgress()
        }
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            // Avatar
            val colorRes = avatarColorRes(profile.avatarColorIndex)
            binding.tvAvatarLetter.text = profile.avatarLetter
            binding.viewAvatarBg.setBackgroundColor(
                requireContext().getColor(colorRes)
            )
            // Sélection
            avatarButtons.forEachIndexed { i, btn ->
                btn.alpha = if (i == profile.avatarColorIndex) 1f else 0.35f
            }

            // Nom
            if (binding.etDisplayName.text.isNullOrEmpty()) {
                binding.etDisplayName.setText(profile.displayName)
            }

            // Code ami
            binding.tvFriendCode.text = profile.friendCode

            // Visibilité
            binding.switchPublic.isChecked = profile.isPublic
            binding.tvPublicHint.text = if (profile.isPublic)
                getString(R.string.profile_public_hint_on)
            else
                getString(R.string.profile_public_hint_off)

            // Notifications
            binding.switchMorning.isChecked = profile.notifMorningEnabled
            binding.switchEvening.isChecked = profile.notifEveningEnabled
            binding.sliderMorning.value = profile.notifMorningHour.toFloat()
            binding.sliderEvening.value = profile.notifEveningHour.toFloat()
            updateSliderLabels(profile.notifMorningHour, profile.notifEveningHour)
        }

        viewModel.streak.observe(viewLifecycleOwner) { streak ->
            binding.tvStreakValue.text = streak.count.toString()
            binding.tvStreakBadge.text = streak.badge
            binding.tvStreakRecord.text = getString(R.string.streak_record, streak.longestEver)
        }

        viewModel.successMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessages()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        }
    }

    // ── Partage ───────────────────────────────────────────────────────────────

    private fun shareProgress() {
        val text = viewModel.buildShareText(
            completedToday = 0, // TODO: récupérer depuis TaskViewModel partagé
            totalToday = 0
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_progress_title)))
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private fun updateSliderLabels(morningHour: Int, eveningHour: Int) {
        binding.tvMorningHour.text = getString(R.string.hour_format, morningHour)
        binding.tvEveningHour.text = getString(R.string.hour_format, eveningHour)
    }

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

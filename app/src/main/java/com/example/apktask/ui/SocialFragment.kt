package com.example.apktask.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apktask.R
import com.example.apktask.databinding.FragmentSocialBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

/**
 * Fragment de l'onglet Social.
 *
 * Affiche :
 *  - La progression journalière des amis (vrais ou démo)
 *  - Un badge "Démo" quand Firebase n'est pas configuré
 *  - Le bouton pour ajouter un ami via son code
 *  - Un écran vide incitatif si aucun ami
 */
class SocialFragment : Fragment() {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SocialViewModel by viewModels()
    private lateinit var friendsAdapter: FriendAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        friendsAdapter = FriendAdapter(
            onRemoveFriend = { userId ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.remove_friend_title)
                    .setMessage(R.string.remove_friend_confirm)
                    .setPositiveButton(R.string.button_confirm) { _, _ ->
                        viewModel.removeFriend(userId)
                    }
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()
            }
        )
        binding.rvFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnAddFriend.setOnClickListener {
            showAddFriendDialog()
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.loadFriends()
        }
    }

    // ── Observation ───────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.friends.observe(viewLifecycleOwner) { friends ->
            friendsAdapter.submitList(friends)

            val hasMock = friends.any { it.isMock }
            binding.bannerDemo.visibility = if (hasMock) View.VISIBLE else View.GONE
            binding.layoutEmpty.visibility =
                if (friends.isEmpty() && !hasMock) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
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

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private fun showAddFriendDialog() {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.hint_friend_code)
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_friend_title)
            .setMessage(R.string.add_friend_message)
            .setView(input)
            .setPositiveButton(R.string.button_add) { _, _ ->
                viewModel.addFriend(input.text.toString())
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }
}

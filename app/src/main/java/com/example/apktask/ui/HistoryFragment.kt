package com.example.apktask.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apktask.R
import com.example.apktask.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

/**
 * Fragment de l'onglet Historique.
 *
 * Affiche :
 *  - Les statistiques globales (jours suivis, moyenne, jours parfaits, tâches accomplies)
 *  - La série actuelle et le record personnel
 *  - La liste des 30 derniers bilans journaliers
 *  - Un état vide incitatif si aucun bilan n'existe encore
 */
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: DaySummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = DaySummaryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@HistoryFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.summaries.collect { summaries ->
                        adapter.submitList(summaries)
                        binding.layoutEmpty.visibility =
                            if (summaries.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvHistory.visibility =
                            if (summaries.isEmpty()) View.GONE else View.VISIBLE
                        binding.cardStats.visibility =
                            if (summaries.isEmpty()) View.GONE else View.VISIBLE
                        binding.tvRecentTitle.visibility =
                            if (summaries.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.stats.collect { stats ->
                        binding.tvStatDays.text = stats.totalDaysTracked.toString()
                        binding.tvStatAvg.text = getString(R.string.history_percent, stats.averageCompletion)
                        binding.tvStatPerfect.text = stats.perfectDays.toString()
                        binding.tvStatCompleted.text = stats.totalTasksCompleted.toString()
                    }
                }

                launch {
                    viewModel.streak.collect { streak ->
                        val icon = streak.badge.ifEmpty { "\u2B50" }
                        binding.tvStreakIcon.text = icon
                        binding.tvStreakValue.text = getString(
                            R.string.history_streak_value, streak.count
                        )
                        binding.tvStreakRecord.text = getString(
                            R.string.history_streak_record_value, streak.longestEver
                        )
                    }
                }
            }
        }
    }
}

package com.minepapa.kidsmoneyapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.minepapa.kidsmoneyapp.databinding.DialogAddGoalBinding
import com.minepapa.kidsmoneyapp.databinding.FragmentGoalsBinding
import java.time.LocalDate
import java.time.YearMonth

class GoalsFragment : Fragment() {

    private var _binding: FragmentGoalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DatabaseHelper(requireContext())

        goalAdapter = GoalAdapter(
            emptyList(),
            onDelete = { goal ->
                AlertDialog.Builder(requireContext())
                    .setTitle("목표 삭제")
                    .setMessage("'${goal.title}' 목표를 삭제할까요?")
                    .setPositiveButton("삭제") { _, _ ->
                        db.deleteGoal(goal.id)
                        loadGoals()
                    }
                    .setNegativeButton("취소", null)
                    .show()
            },
            onPurchaseToggle = { goal ->
                db.markGoalPurchased(goal.id, !goal.purchased)
                AchievementManager.checkAndUnlock(db)
                loadGoals()
                loadBadges()
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoals.adapter = goalAdapter

        badgeAdapter = BadgeAdapter(emptyList())
        binding.rvBadges.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvBadges.adapter = badgeAdapter

        binding.btnAddGoal.setOnClickListener { showAddGoalDialog() }

        loadGoals()
        loadBadges()
    }

    override fun onResume() {
        super.onResume()
        loadGoals()
        loadBadges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadGoals() {
        val goals = db.getGoalsForMonth(YearMonth.now().toString())
        goalAdapter.update(goals)
        binding.tvNoGoals.visibility = if (goals.isEmpty()) View.VISIBLE else View.GONE
        binding.rvGoals.visibility = if (goals.isEmpty()) View.GONE else View.VISIBLE

        goals.filter { !it.completed && it.savedAmount >= it.targetAmount }.forEach { goal ->
            db.markGoalCompleted(goal.id)
            AlertDialog.Builder(requireContext())
                .setTitle("🎉 목표 달성!")
                .setMessage("'${goal.title}' 목표를 달성했어요!\n정말 대단해요!")
                .setPositiveButton("신나요!", null)
                .show()
            AchievementManager.checkAndUnlock(db)
        }
    }

    private fun loadBadges() {
        val achievements = AchievementManager.buildAchievementList(db)
        badgeAdapter.update(achievements)
    }

    private fun showAddGoalDialog() {
        val dialogView = DialogAddGoalBinding.inflate(layoutInflater)
        AlertDialog.Builder(requireContext())
            .setView(dialogView.root)
            .setPositiveButton("추가") { _, _ ->
                val title = dialogView.etGoalTitle.text.toString()
                val amountStr = dialogView.etGoalAmount.text.toString()
                if (title.isBlank() || amountStr.isBlank()) return@setPositiveButton
                val amount = amountStr.toIntOrNull() ?: return@setPositiveButton
                db.insertGoal(SavingsGoal(title = title, targetAmount = amount, createdDate = LocalDate.now().toString()))
                loadGoals()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}

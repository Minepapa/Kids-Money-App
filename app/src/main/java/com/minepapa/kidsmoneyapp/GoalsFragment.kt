package com.minepapa.kidsmoneyapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
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
            onAddSavings = { goal, amount ->
                db.updateGoalSaved(goal.id, amount)
                loadGoals()
                AchievementManager.checkAndUnlock(db)
            }
        )
        binding.rvGoals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoals.adapter = goalAdapter

        badgeAdapter = BadgeAdapter(emptyList())
        binding.rvBadges.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.rvBadges.adapter = badgeAdapter

        binding.btnAddGoal.setOnClickListener { showAddGoalDialog() }

        loadGoals()
        loadChart()
        loadBadges()
    }

    override fun onResume() {
        super.onResume()
        loadGoals()
        loadChart()
        loadBadges()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadGoals() {
        val goals = db.getAllGoals()
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

    private fun loadChart() {
        val currentMonth = YearMonth.now().toString()
        val records = db.getAllRecords().filter { it.date.startsWith(currentMonth) }
        var inc = 0f; var exp = 0f; var bnk = 0f
        records.forEach {
            when (it.type) {
                "income", "frombank" -> inc += it.amount
                "expense" -> exp += it.amount
                "tobank", "direct_in", "interest" -> bnk += it.amount
            }
        }

        if (inc == 0f && exp == 0f && bnk == 0f) {
            binding.pieChart.visibility = View.GONE
            binding.tvNoChart.visibility = View.VISIBLE
            return
        }

        binding.pieChart.visibility = View.VISIBLE
        binding.tvNoChart.visibility = View.GONE

        val entries = mutableListOf<PieEntry>()
        if (inc > 0) entries.add(PieEntry(inc, "수입"))
        if (exp > 0) entries.add(PieEntry(exp, "지출"))
        if (bnk > 0) entries.add(PieEntry(bnk, "저축"))

        val colors = mutableListOf<Int>()
        if (inc > 0) colors.add(Color.parseColor("#2ecc71"))
        if (exp > 0) colors.add(Color.parseColor("#e74c3c"))
        if (bnk > 0) colors.add(Color.parseColor("#d4ac0d"))

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.WHITE
        }
        val pieData = PieData(dataSet)
        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            setUsePercentValues(true)
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = true
            animateY(600)
            invalidate()
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

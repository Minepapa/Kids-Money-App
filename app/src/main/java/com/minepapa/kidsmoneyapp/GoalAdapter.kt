package com.minepapa.kidsmoneyapp

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.minepapa.kidsmoneyapp.databinding.ItemGoalBinding

class GoalAdapter(
    private var goals: List<SavingsGoal>,
    private val onDelete: (SavingsGoal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goal: SavingsGoal) {
            binding.tvGoalTitle.text = if (goal.completed) "✅ ${goal.title}" else goal.title
            binding.tvGoalAmount.text = "${goal.savedAmount.formatted()}원 / ${goal.targetAmount.formatted()}원"
            binding.progressGoal.progress = goal.progressPercent
            binding.tvGoalPercent.text = "${goal.progressPercent}% 달성"
            binding.tvGoalRemaining.text = if (goal.completed) "🎉 목표 달성!" else "남은 금액: ${goal.remaining.formatted()}원"

            binding.btnDeleteGoal.setOnClickListener { onDelete(goal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(goals[position])
    override fun getItemCount() = goals.size

    fun update(newGoals: List<SavingsGoal>) {
        goals = newGoals
        notifyDataSetChanged()
    }

    private fun Int.formatted() = String.format("%,d", this)
}

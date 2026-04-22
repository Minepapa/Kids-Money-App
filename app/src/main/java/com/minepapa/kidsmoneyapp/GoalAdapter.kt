package com.minepapa.kidsmoneyapp

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.minepapa.kidsmoneyapp.databinding.ItemGoalBinding

class GoalAdapter(
    private var goals: List<SavingsGoal>,
    private val onDelete: (SavingsGoal) -> Unit,
    private val onAddSavings: (SavingsGoal, Int) -> Unit
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goal: SavingsGoal) {
            binding.tvGoalTitle.text = if (goal.completed) "✅ ${goal.title}" else goal.title
            binding.tvGoalAmount.text = "${goal.savedAmount.formatted()}원 / ${goal.targetAmount.formatted()}원"
            binding.progressGoal.progress = goal.progressPercent
            binding.tvGoalPercent.text = "${goal.progressPercent}% 달성"
            binding.tvGoalRemaining.text = if (goal.completed) "🎉 목표 달성!" else "남은 금액: ${goal.remaining.formatted()}원"

            binding.btnDeleteGoal.setOnClickListener { onDelete(goal) }
            binding.btnAddSavings.setOnClickListener {
                val input = EditText(binding.root.context).apply {
                    hint = "저금할 금액 (원)"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                }
                AlertDialog.Builder(binding.root.context)
                    .setTitle("💰 저금하기")
                    .setMessage("'${goal.title}'에 얼마를 저금할까요?")
                    .setView(input)
                    .setPositiveButton("저금") { _, _ ->
                        val amount = input.text.toString().toIntOrNull() ?: return@setPositiveButton
                        if (amount > 0) onAddSavings(goal, amount)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
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

package com.minepapa.kidsmoneyapp

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.minepapa.kidsmoneyapp.databinding.ItemGoalBinding

class GoalAdapter(
    private var goals: List<SavingsGoal>,
    private val onDelete: (SavingsGoal) -> Unit,
    private val onPurchaseToggle: (SavingsGoal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goal: SavingsGoal) {
            binding.tvGoalAmount.text = "${goal.targetAmount.formatted()}원"
            binding.tvGoalPurchased.text = if (goal.purchased) "✅" else "🎯"

            val ctx = binding.root.context
            if (goal.purchased) {
                binding.tvGoalTitle.text = goal.title
                binding.tvGoalTitle.paintFlags = binding.tvGoalTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvGoalTitle.setTextColor(ctx.getColor(R.color.sb_text_hint))
                binding.tvGoalAmount.setTextColor(ctx.getColor(R.color.sb_text_hint))
            } else {
                binding.tvGoalTitle.text = goal.title
                binding.tvGoalTitle.paintFlags = binding.tvGoalTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvGoalTitle.setTextColor(ctx.getColor(R.color.sb_text_primary))
                binding.tvGoalAmount.setTextColor(ctx.getColor(R.color.sb_text_secondary))
            }

            binding.tvGoalPurchased.setOnClickListener { onPurchaseToggle(goal) }
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

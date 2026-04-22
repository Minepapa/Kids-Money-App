package com.minepapa.kidsmoneyapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.minepapa.kidsmoneyapp.databinding.ItemBadgeBinding

class BadgeAdapter(
    private var achievements: List<Achievement>
) : RecyclerView.Adapter<BadgeAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(achievement: Achievement) {
            binding.tvBadgeEmoji.text = achievement.emoji
            binding.tvBadgeName.text = achievement.titleKo
            if (achievement.isUnlocked) {
                binding.root.alpha = 1.0f
                binding.tvBadgeName.setTextColor(0xFF333333.toInt())
            } else {
                binding.root.alpha = 0.3f
                binding.tvBadgeName.setTextColor(0xFF999999.toInt())
            }

            // 길게 누르면 획득 조건 설명 표시
            binding.root.setOnLongClickListener {
                AlertDialog.Builder(it.context)
                    .setTitle("${achievement.emoji} ${achievement.titleKo}")
                    .setMessage(achievement.descriptionKo)
                    .setPositiveButton("확인", null)
                    .show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(achievements[position])
    override fun getItemCount() = achievements.size

    fun update(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }
}

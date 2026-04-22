package com.minepapa.kidsmoneyapp

data class SavingsGoal(
    val id: Long = 0,
    val title: String,
    val targetAmount: Int,
    val savedAmount: Int = 0,
    val createdDate: String,
    val completed: Boolean = false,
    val purchased: Boolean = false
) {
    val progress: Float get() = if (targetAmount == 0) 0f else savedAmount.toFloat() / targetAmount
    val progressPercent: Int get() = (progress * 100).toInt().coerceAtMost(100)
    val remaining: Int get() = maxOf(0, targetAmount - savedAmount)
}

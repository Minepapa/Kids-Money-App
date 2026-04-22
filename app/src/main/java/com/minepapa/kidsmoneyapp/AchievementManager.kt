package com.minepapa.kidsmoneyapp

import java.time.LocalDate

object AchievementManager {

    val catalog: List<Achievement> = listOf(
        Achievement("first_record",    "🌱", "첫 기록!",     "처음으로 용돈을 기록했어요"),
        Achievement("records_10",      "📝", "기록왕",       "용돈을 10번 기록했어요"),
        Achievement("records_50",      "📚", "기록 달인",    "용돈을 50번 기록했어요"),
        Achievement("streak_3",        "🔥", "3일 연속!",    "3일 연속으로 기록했어요"),
        Achievement("streak_7",        "⭐", "일주일 연속!", "7일 연속으로 기록했어요"),
        Achievement("streak_30",       "🏆", "한 달 연속!",  "30일 연속으로 기록했어요"),
        Achievement("saved_5000",      "💰", "저축 시작",    "금고에 5,000원 모았어요"),
        Achievement("saved_10000",     "💎", "만원 부자",    "금고에 10,000원 모았어요"),
        Achievement("saved_50000",     "👑", "저축 왕",      "금고에 50,000원 모았어요"),
        Achievement("goal_first",      "🎯", "목표 달성!",   "저축 목표를 처음 달성했어요"),
        Achievement("goal_3",          "🎖️", "목표 달인",   "저축 목표를 3번 달성했어요"),
        Achievement("no_expense_week", "🌈", "절약왕",       "일주일 동안 지출이 없었어요"),
        Achievement("income_record",   "💫", "용돈 부자",    "한 달에 수입이 50,000원이 넘었어요"),
        Achievement("interest_first",  "🌟", "이자 첫 수령!", "아빠 금고에서 이자를 받았어요"),
        Achievement("interest_1000",   "💹", "이자 부자",    "이자를 1,000원 이상 받았어요")
    )

    private val catalogMap = catalog.associateBy { it.id }

    fun checkAndUnlock(db: DatabaseHelper): List<Achievement> {
        val today = LocalDate.now().toString()
        val alreadyUnlocked = db.getUnlockedAchievementIds()
        val newlyUnlocked = mutableListOf<Achievement>()

        fun tryUnlock(id: String) {
            if (id !in alreadyUnlocked) {
                db.unlockAchievement(id, today)
                catalogMap[id]?.let { newlyUnlocked.add(it) }
            }
        }

        val allRecords = db.getAllRecords()
        val totalCount = allRecords.size

        if (totalCount >= 1)  tryUnlock("first_record")
        if (totalCount >= 10) tryUnlock("records_10")
        if (totalCount >= 50) tryUnlock("records_50")

        val streak = db.getRecordingStreak()
        if (streak >= 3)  tryUnlock("streak_3")
        if (streak >= 7)  tryUnlock("streak_7")
        if (streak >= 30) tryUnlock("streak_30")

        var bank = 0
        allRecords.forEach {
            when (it.type) {
                "tobank", "direct_in", "interest" -> bank += it.amount
                "frombank" -> bank -= it.amount
            }
        }
        if (bank >= 5000)  tryUnlock("saved_5000")
        if (bank >= 10000) tryUnlock("saved_10000")
        if (bank >= 50000) tryUnlock("saved_50000")

        val completedGoals = db.countCompletedGoals()
        if (completedGoals >= 1) tryUnlock("goal_first")
        if (completedGoals >= 3) tryUnlock("goal_3")

        val hasNoExpenseWeek = checkNoExpenseWeek(allRecords)
        if (hasNoExpenseWeek) tryUnlock("no_expense_week")

        val monthlyIncome = allRecords
            .filter { it.type == "income" && it.date.startsWith(today.substring(0, 7)) }
            .sumOf { it.amount }
        if (monthlyIncome >= 50000) tryUnlock("income_record")

        val interestRecords = allRecords.filter { it.type == "interest" }
        if (interestRecords.isNotEmpty()) tryUnlock("interest_first")
        if (interestRecords.sumOf { it.amount } >= 1000) tryUnlock("interest_1000")

        return newlyUnlocked
    }

    fun buildAchievementList(db: DatabaseHelper): List<Achievement> {
        val unlockedIds = db.getUnlockedAchievementIds()
        val unseenIds = db.getUnseenAchievements().toSet()
        return catalog.map { a ->
            val isUnlocked = a.id in unlockedIds
            a.copy(
                unlockedDate = if (isUnlocked) "unlocked" else null,
                seen = a.id !in unseenIds
            )
        }
    }

    private fun checkNoExpenseWeek(records: List<Record>): Boolean {
        val expenseDates = records.filter { it.isExpense }
            .mapNotNull { runCatching { java.time.LocalDate.parse(it.date) }.getOrNull() }
            .toSet()
        val today = java.time.LocalDate.now()
        for (start in 0..90) {
            val weekStart = today.minusDays(start.toLong() + 6)
            val weekEnd = today.minusDays(start.toLong())
            val hasExpense = expenseDates.any { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
            if (!hasExpense) return true
        }
        return false
    }
}

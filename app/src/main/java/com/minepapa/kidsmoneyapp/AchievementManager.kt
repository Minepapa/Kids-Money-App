package com.minepapa.kidsmoneyapp

import java.time.LocalDate
import java.time.YearMonth

object AchievementManager {

    val catalog: List<Achievement> = listOf(
        Achievement("first_record",    "🌱", "첫 기록!",     "이번 달 처음으로 용돈을 기록해요 (1건)"),
        Achievement("records_10",      "📝", "기록왕",       "이번 달 용돈을 10번 기록해요"),
        Achievement("records_20",      "📚", "기록 달인",    "이번 달 용돈을 20번 기록해요"),
        Achievement("streak_3",        "🔥", "3일 연속!",    "이번 달 3일 연속으로 기록해요"),
        Achievement("streak_7",        "⭐", "일주일 연속!", "이번 달 7일 연속으로 기록해요"),
        Achievement("streak_15",       "🏆", "15일 연속!",   "이번 달 15일 이상 기록해요"),
        Achievement("saved_5000",      "💰", "저축 시작",    "이번 달 금고에 5,000원 이상 저축해요"),
        Achievement("saved_10000",     "💎", "만원 부자",    "이번 달 금고에 10,000원 이상 저축해요"),
        Achievement("saved_30000",     "👑", "저축 왕",      "이번 달 금고에 30,000원 이상 저축해요"),
        Achievement("goal_first",      "🎯", "목표 달성!",   "이번 달 저축 목표를 1개 달성해요"),
        Achievement("goal_2",          "🎖️", "목표 달인",   "이번 달 저축 목표를 2개 달성해요"),
        Achievement("no_expense_7",    "🌈", "절약왕",       "이번 달 지출이 없는 날이 7일 이상이에요"),
        Achievement("income_record",   "💫", "용돈 부자",    "이번 달 수입이 50,000원이 넘어요"),
        Achievement("interest_first",  "🌟", "이자 수령!",   "이번 달 아빠 금고에서 이자를 받아요"),
        Achievement("interest_1000",   "💹", "이자 부자",    "이번 달 이자를 1,000원 이상 받아요")
    )

    private val catalogMap = catalog.associateBy { it.id }

    fun checkAndUnlock(db: DatabaseHelper): List<Achievement> {
        val today = LocalDate.now().toString()
        val currentMonth = YearMonth.now().toString()
        val alreadyUnlocked = db.getUnlockedAchievementIds(currentMonth)
        val newlyUnlocked = mutableListOf<Achievement>()

        fun tryUnlock(id: String) {
            if (id !in alreadyUnlocked) {
                db.unlockAchievement(id, today, currentMonth)
                catalogMap[id]?.let { newlyUnlocked.add(it) }
            }
        }

        // 이번 달 기록만 사용
        val monthRecords = db.getAllRecords().filter { it.date.startsWith(currentMonth) }
        val totalCount = monthRecords.size

        if (totalCount >= 1)  tryUnlock("first_record")
        if (totalCount >= 10) tryUnlock("records_10")
        if (totalCount >= 20) tryUnlock("records_20")

        // 연속 기록 (전체 streak 사용, 이번 달 포함 여부)
        val streak = db.getRecordingStreak()
        if (streak >= 3)  tryUnlock("streak_3")
        if (streak >= 7)  tryUnlock("streak_7")
        if (streak >= 15) tryUnlock("streak_15")

        // 이번 달 금고 저축 합계
        val monthBank = monthRecords
            .filter { it.type == "tobank" || it.type == "direct_in" }
            .sumOf { it.amount }
        if (monthBank >= 5000)  tryUnlock("saved_5000")
        if (monthBank >= 10000) tryUnlock("saved_10000")
        if (monthBank >= 30000) tryUnlock("saved_30000")

        // 이번 달 목표 달성
        val completedGoals = db.countCompletedGoalsInMonth(currentMonth)
        if (completedGoals >= 1) tryUnlock("goal_first")
        if (completedGoals >= 2) tryUnlock("goal_2")

        // 이번 달 지출 없는 날 7일 이상
        val expenseDatesThisMonth = monthRecords
            .filter { it.isExpense }
            .map { it.date }
            .toSet()
        val daysInMonth = YearMonth.now().lengthOfMonth()
        val noExpenseDays = (1..daysInMonth).count { day ->
            val d = "%s-%02d".format(currentMonth, day)
            d !in expenseDatesThisMonth
        }
        if (noExpenseDays >= 7) tryUnlock("no_expense_7")

        // 이번 달 수입 합계
        val monthIncome = monthRecords.filter { it.type == "income" }.sumOf { it.amount }
        if (monthIncome >= 50000) tryUnlock("income_record")

        // 이번 달 이자
        val monthInterest = monthRecords.filter { it.type == "interest" }
        if (monthInterest.isNotEmpty()) tryUnlock("interest_first")
        if (monthInterest.sumOf { it.amount } >= 1000) tryUnlock("interest_1000")

        return newlyUnlocked
    }

    fun buildAchievementList(db: DatabaseHelper): List<Achievement> {
        val currentMonth = YearMonth.now().toString()
        val unlockedIds = db.getUnlockedAchievementIds(currentMonth)
        val unseenIds = db.getUnseenAchievements(currentMonth).toSet()
        return catalog.map { a ->
            val isUnlocked = a.id in unlockedIds
            a.copy(
                unlockedDate = if (isUnlocked) "unlocked" else null,
                seen = a.id !in unseenIds
            )
        }
    }
}

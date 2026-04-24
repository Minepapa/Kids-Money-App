package com.minepapa.kidsmoneyapp

import java.time.LocalDate
import java.time.YearMonth

object AchievementManager {

    val catalog: List<Achievement> = listOf(
        Achievement("first_record",    "🌱", "첫 기록!",       "이번 달 처음으로 용돈을 기록해요 (1건)"),
        Achievement("records_10",      "📝", "기록왕",         "이번 달 용돈을 10번 기록해요"),
        Achievement("records_20",      "📚", "기록 달인",      "이번 달 용돈을 20번 기록해요"),
        Achievement("records_30",      "📖", "기록 마스터",    "이번 달 용돈을 30번 기록해요"),
        Achievement("streak_3",        "🔥", "3일 연속!",      "이번 달 3일 연속으로 기록해요"),
        Achievement("streak_7",        "⭐", "일주일 연속!",   "이번 달 7일 연속으로 기록해요"),
        Achievement("streak_15",       "🏆", "15일 연속!",     "이번 달 15일 이상 기록해요"),
        Achievement("saved_5000",      "💰", "저축 시작",      "이번 달 금고에 5,000원 이상 저축해요"),
        Achievement("saved_10000",     "💎", "만원 부자",      "이번 달 금고에 10,000원 이상 저축해요"),
        Achievement("saved_30000",     "👑", "저축 왕",        "이번 달 금고에 30,000원 이상 저축해요"),
        Achievement("bank_50000",      "🏦", "금고 부자",      "아빠 금고에 50,000원 이상 쌓여있어요"),
        Achievement("goal_first",      "🎯", "첫 구매!",       "이번 달 저축 목표 1개를 구매해요"),
        Achievement("goal_2",          "🎖️", "쇼핑왕",        "이번 달 저축 목표 2개를 구매해요"),
        Achievement("goal_3",          "🎁", "꿈 이루기",      "이번 달 저축 목표 3개를 구매해요"),
        Achievement("no_expense_7",    "🌈", "절약왕",         "이번 달 지출이 없는 날이 7일 이상이에요"),
        Achievement("no_expense_14",   "🌙", "절약 챔피언",    "이번 달 지출이 없는 날이 14일 이상이에요"),
        Achievement("income_record",   "💫", "용돈 부자",      "이번 달 수입이 50,000원이 넘어요"),
        Achievement("big_income",      "💵", "큰 용돈",        "하루에 10,000원 이상 수입이 생겨요"),
        Achievement("interest_first",  "🌟", "이자 수령!",     "이번 달 아빠 금고에서 이자를 받아요"),
        Achievement("interest_1000",   "💹", "이자 부자",      "이번 달 이자를 1,000원 이상 받아요")
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

        val monthRecords = db.getAllRecords().filter { it.date.startsWith(currentMonth) }
        val totalCount = monthRecords.size

        if (totalCount >= 1)  tryUnlock("first_record")
        if (totalCount >= 10) tryUnlock("records_10")
        if (totalCount >= 20) tryUnlock("records_20")
        if (totalCount >= 30) tryUnlock("records_30")

        val streak = db.getRecordingStreak()
        if (streak >= 3)  tryUnlock("streak_3")
        if (streak >= 7)  tryUnlock("streak_7")
        if (streak >= 15) tryUnlock("streak_15")

        val monthBank = monthRecords
            .filter { it.type == "tobank" || it.type == "direct_in" }
            .sumOf { it.amount }
        if (monthBank >= 5000)  tryUnlock("saved_5000")
        if (monthBank >= 10000) tryUnlock("saved_10000")
        if (monthBank >= 30000) tryUnlock("saved_30000")

        val (bankPrincipal, bankInterest) = db.getBankBalancesBreakdown()
        if (bankPrincipal + bankInterest >= 50000) tryUnlock("bank_50000")

        // 저축 목표 구매 개수 (달성 아이콘 클릭 횟수)
        val purchasedGoals = db.countPurchasedGoalsInMonth(currentMonth)
        if (purchasedGoals >= 1) tryUnlock("goal_first")
        if (purchasedGoals >= 2) tryUnlock("goal_2")
        if (purchasedGoals >= 3) tryUnlock("goal_3")

        val expenseDatesThisMonth = monthRecords
            .filter { it.isExpense }
            .map { it.date }
            .toSet()
        val daysInMonth = YearMonth.now().lengthOfMonth()
        val noExpenseDays = (1..daysInMonth).count { day ->
            val d = "%s-%02d".format(currentMonth, day)
            d !in expenseDatesThisMonth
        }
        if (noExpenseDays >= 7)  tryUnlock("no_expense_7")
        if (noExpenseDays >= 14) tryUnlock("no_expense_14")

        val monthIncome = monthRecords.filter { it.type == "income" }.sumOf { it.amount }
        if (monthIncome >= 50000) tryUnlock("income_record")

        val maxDayIncome = monthRecords.filter { it.type == "income" }
            .groupBy { it.date }
            .maxOfOrNull { (_, recs) -> recs.sumOf { it.amount } } ?: 0
        if (maxDayIncome >= 10000) tryUnlock("big_income")

        val monthInterest = monthRecords.filter { it.type == "interest" }
        if (monthInterest.isNotEmpty()) tryUnlock("interest_first")
        if (monthInterest.sumOf { it.amount } >= 1000) tryUnlock("interest_1000")

        return newlyUnlocked
    }

    fun buildAchievementList(db: DatabaseHelper): List<Achievement> {
        val currentMonth = YearMonth.now().toString()
        val activeIds = computeCurrentlyActiveIds(db, currentMonth)
        val unseenIds = db.getUnseenAchievements(currentMonth).toSet()
        return catalog.map { a ->
            a.copy(
                unlockedDate = if (a.id in activeIds) "unlocked" else null,
                seen = a.id !in unseenIds
            )
        }
    }

    private fun computeCurrentlyActiveIds(db: DatabaseHelper, currentMonth: String): Set<String> {
        val active = mutableSetOf<String>()
        val monthRecords = db.getAllRecords().filter { it.date.startsWith(currentMonth) }
        val totalCount = monthRecords.size

        if (totalCount >= 1)  active.add("first_record")
        if (totalCount >= 10) active.add("records_10")
        if (totalCount >= 20) active.add("records_20")
        if (totalCount >= 30) active.add("records_30")

        val streak = db.getRecordingStreak()
        if (streak >= 3)  active.add("streak_3")
        if (streak >= 7)  active.add("streak_7")
        if (streak >= 15) active.add("streak_15")

        val monthBank = monthRecords
            .filter { it.type == "tobank" || it.type == "direct_in" }
            .sumOf { it.amount }
        if (monthBank >= 5000)  active.add("saved_5000")
        if (monthBank >= 10000) active.add("saved_10000")
        if (monthBank >= 30000) active.add("saved_30000")

        val (bankPrincipal, bankInterest) = db.getBankBalancesBreakdown()
        if (bankPrincipal + bankInterest >= 50000) active.add("bank_50000")

        val purchasedGoals = db.countPurchasedGoalsInMonth(currentMonth)
        if (purchasedGoals >= 1) active.add("goal_first")
        if (purchasedGoals >= 2) active.add("goal_2")
        if (purchasedGoals >= 3) active.add("goal_3")

        val expenseDatesThisMonth = monthRecords
            .filter { it.isExpense }
            .map { it.date }
            .toSet()
        val daysInMonth = YearMonth.now().lengthOfMonth()
        val noExpenseDays = (1..daysInMonth).count { day ->
            val d = "%s-%02d".format(currentMonth, day)
            d !in expenseDatesThisMonth
        }
        if (noExpenseDays >= 7)  active.add("no_expense_7")
        if (noExpenseDays >= 14) active.add("no_expense_14")

        val monthIncome = monthRecords.filter { it.type == "income" }.sumOf { it.amount }
        if (monthIncome >= 50000) active.add("income_record")

        val maxDayIncome = monthRecords.filter { it.type == "income" }
            .groupBy { it.date }
            .maxOfOrNull { (_, recs) -> recs.sumOf { it.amount } } ?: 0
        if (maxDayIncome >= 10000) active.add("big_income")

        val monthInterest = monthRecords.filter { it.type == "interest" }
        if (monthInterest.isNotEmpty()) active.add("interest_first")
        if (monthInterest.sumOf { it.amount } >= 1000) active.add("interest_1000")

        return active
    }
}

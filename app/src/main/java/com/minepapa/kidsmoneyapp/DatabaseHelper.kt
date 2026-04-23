package com.minepapa.kidsmoneyapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.YearMonth

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "kids_money.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                type TEXT NOT NULL,
                memo TEXT NOT NULL,
                amount INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE savings_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                target_amount INTEGER NOT NULL,
                saved_amount INTEGER NOT NULL DEFAULT 0,
                created_date TEXT NOT NULL,
                completed INTEGER NOT NULL DEFAULT 0,
                purchased INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE achievements (
                id TEXT NOT NULL,
                month TEXT NOT NULL DEFAULT '',
                unlocked_date TEXT,
                seen INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (id, month)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS savings_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    target_amount INTEGER NOT NULL,
                    saved_amount INTEGER NOT NULL DEFAULT 0,
                    created_date TEXT NOT NULL,
                    completed INTEGER NOT NULL DEFAULT 0
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS achievements (
                    id TEXT PRIMARY KEY,
                    unlocked_date TEXT,
                    seen INTEGER NOT NULL DEFAULT 0
                )
            """)
        }
        if (oldVersion < 3) {
            runCatching { db.execSQL("ALTER TABLE achievements ADD COLUMN month TEXT NOT NULL DEFAULT ''") }
        }
        if (oldVersion < 4) {
            runCatching { db.execSQL("ALTER TABLE savings_goals ADD COLUMN purchased INTEGER NOT NULL DEFAULT 0") }
        }
    }

    // ── Records ──────────────────────────────────────────────────────────────

    fun insertRecord(record: Record): Long {
        val values = ContentValues().apply {
            put("date", record.date)
            put("type", record.type)
            put("memo", record.memo)
            put("amount", record.amount)
        }
        return writableDatabase.insert("records", null, values)
    }

    fun deleteRecord(id: Long) {
        writableDatabase.delete("records", "id=?", arrayOf(id.toString()))
    }

    fun getAllRecords(): List<Record> {
        val list = mutableListOf<Record>()
        readableDatabase.query("records", null, null, null, null, null, "date ASC").use {
            while (it.moveToNext()) {
                list.add(Record(it.getLong(0), it.getString(1), it.getString(2), it.getString(3), it.getInt(4)))
            }
        }
        return list
    }

    fun getRecordsByDate(date: String): List<Record> {
        val list = mutableListOf<Record>()
        readableDatabase.query("records", null, "date=?", arrayOf(date), null, null, "id ASC").use {
            while (it.moveToNext()) {
                list.add(Record(it.getLong(0), it.getString(1), it.getString(2), it.getString(3), it.getInt(4)))
            }
        }
        return list
    }

    fun getRecordingStreak(): Int {
        val dates = mutableListOf<LocalDate>()
        readableDatabase.rawQuery(
            "SELECT DISTINCT date FROM records ORDER BY date DESC", null
        ).use {
            while (it.moveToNext()) {
                runCatching { LocalDate.parse(it.getString(0)) }.onSuccess { d -> dates.add(d) }
            }
        }
        var streak = 0
        var expected = LocalDate.now()
        for (d in dates) {
            if (d == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (d.isBefore(expected)) {
                break
            }
        }
        return streak
    }

    fun getBankBalancesBreakdown(): Pair<Int, Int> {
        var principal = 0
        var interest = 0
        getAllRecords().forEach {
            when (it.type) {
                "tobank", "direct_in" -> principal += it.amount
                "frombank" -> principal -= it.amount
                "interest" -> interest += it.amount
            }
        }
        return Pair(principal.coerceAtLeast(0), interest)
    }

    // ── Savings Goals ─────────────────────────────────────────────────────────

    fun insertGoal(goal: SavingsGoal): Long {
        val values = ContentValues().apply {
            put("title", goal.title)
            put("target_amount", goal.targetAmount)
            put("saved_amount", goal.savedAmount)
            put("created_date", goal.createdDate)
            put("completed", if (goal.completed) 1 else 0)
        }
        return writableDatabase.insert("savings_goals", null, values)
    }

    fun getAllGoals(): List<SavingsGoal> {
        val list = mutableListOf<SavingsGoal>()
        readableDatabase.query("savings_goals", null, null, null, null, null, "purchased ASC, id ASC").use {
            while (it.moveToNext()) {
                list.add(
                    SavingsGoal(
                        id = it.getLong(0),
                        title = it.getString(1),
                        targetAmount = it.getInt(2),
                        savedAmount = it.getInt(3),
                        createdDate = it.getString(4),
                        completed = it.getInt(5) == 1,
                        purchased = it.getColumnIndex("purchased").let { col -> if (col >= 0) it.getInt(col) == 1 else false }
                    )
                )
            }
        }
        return list
    }

    fun markGoalPurchased(id: Long, purchased: Boolean) {
        val values = ContentValues().apply { put("purchased", if (purchased) 1 else 0) }
        writableDatabase.update("savings_goals", values, "id=?", arrayOf(id.toString()))
    }

    fun countPurchasedGoalsInMonth(month: String): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM savings_goals WHERE purchased=1 AND created_date LIKE ?",
            arrayOf("$month%")
        ).use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    fun deleteGoal(id: Long) {
        writableDatabase.delete("savings_goals", "id=?", arrayOf(id.toString()))
    }

    fun updateGoalSaved(id: Long, addAmount: Int) {
        writableDatabase.execSQL(
            "UPDATE savings_goals SET saved_amount = saved_amount + ? WHERE id = ?",
            arrayOf(addAmount, id)
        )
    }

    fun markGoalCompleted(id: Long) {
        val values = ContentValues().apply { put("completed", 1) }
        writableDatabase.update("savings_goals", values, "id=?", arrayOf(id.toString()))
    }

    fun countCompletedGoalsInMonth(month: String): Int {
        // 해당 월에 created_date가 있거나, completed된 목표 중 해당 월 기록 기준
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM savings_goals WHERE completed=1 AND created_date LIKE ?",
            arrayOf("$month%")
        ).use {
            if (it.moveToFirst()) return it.getInt(0)
        }
        return 0
    }

    // ── Achievements (월별) ────────────────────────────────────────────────────

    fun isAchievementUnlocked(id: String, month: String): Boolean {
        readableDatabase.query(
            "achievements", arrayOf("unlocked_date"),
            "id=? AND month=?", arrayOf(id, month), null, null, null
        ).use {
            return it.moveToFirst() && !it.isNull(0)
        }
    }

    fun unlockAchievement(id: String, date: String, month: String) {
        val values = ContentValues().apply {
            put("id", id)
            put("month", month)
            put("unlocked_date", date)
            put("seen", 0)
        }
        writableDatabase.insertWithOnConflict("achievements", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun getUnlockedAchievementIds(month: String): Set<String> {
        val ids = mutableSetOf<String>()
        readableDatabase.query(
            "achievements", arrayOf("id"),
            "month=? AND unlocked_date IS NOT NULL", arrayOf(month), null, null, null
        ).use {
            while (it.moveToNext()) ids.add(it.getString(0))
        }
        return ids
    }

    fun getUnseenAchievements(month: String): List<String> {
        val ids = mutableListOf<String>()
        readableDatabase.query(
            "achievements", arrayOf("id"),
            "month=? AND seen=0 AND unlocked_date IS NOT NULL", arrayOf(month), null, null, null
        ).use {
            while (it.moveToNext()) ids.add(it.getString(0))
        }
        return ids
    }

    fun markAchievementSeen(id: String, month: String) {
        val values = ContentValues().apply { put("seen", 1) }
        writableDatabase.update("achievements", values, "id=? AND month=?", arrayOf(id, month))
    }

    fun getLatestUnlockedAchievementId(month: String): String? {
        readableDatabase.query(
            "achievements", arrayOf("id"),
            "month=? AND unlocked_date IS NOT NULL", arrayOf(month),
            null, null, "unlocked_date DESC", "1"
        ).use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    // ── Interest ──────────────────────────────────────────────────────────────

    fun applyMonthlyInterestIfNeeded(ratePercent: Int, lastInterestMonth: String?): String? {
        val currentMonth = YearMonth.now()

        val startMonth = if (lastInterestMonth == null) {
            val firstRecord = getAllRecords().firstOrNull() ?: return null
            runCatching { YearMonth.parse(firstRecord.date.substring(0, 7)) }.getOrNull() ?: return null
        } else {
            runCatching { YearMonth.parse(lastInterestMonth) }.getOrNull()?.plusMonths(1) ?: return null
        }

        if (!startMonth.isBefore(currentMonth)) return null

        var month = startMonth
        while (month.isBefore(currentMonth)) {
            val bankBalance = computeBankBalanceUpTo(month.atEndOfMonth())
            val interest = (bankBalance * ratePercent / 100.0).toInt()
            if (interest > 0) {
                val interestDate = month.atEndOfMonth().toString()
                insertRecord(Record(date = interestDate, type = "interest", memo = "아빠 금고 이자 (월 ${ratePercent}%)", amount = interest))
            }
            month = month.plusMonths(1)
        }
        return currentMonth.minusMonths(1).toString()
    }

    private fun computeBankBalanceUpTo(date: LocalDate): Int {
        var bank = 0
        getAllRecords().forEach { r ->
            val rDate = runCatching { LocalDate.parse(r.date) }.getOrNull() ?: return@forEach
            if (!rDate.isAfter(date)) {
                when (r.type) {
                    "tobank", "direct_in", "interest" -> bank += r.amount
                    "frombank" -> bank -= r.amount
                }
            }
        }
        return bank.coerceAtLeast(0)
    }
}

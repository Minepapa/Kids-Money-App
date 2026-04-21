package com.minepapa.kidsmoneyapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "kids_money.db", null, 1) {

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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS records")
        onCreate(db)
    }

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
        val cursor = readableDatabase.query(
            "records", null, null, null, null, null, "date ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Record(
                        id = it.getLong(0),
                        date = it.getString(1),
                        type = it.getString(2),
                        memo = it.getString(3),
                        amount = it.getInt(4)
                    )
                )
            }
        }
        return list
    }

    fun getRecordsByDate(date: String): List<Record> {
        val list = mutableListOf<Record>()
        val cursor = readableDatabase.query(
            "records", null, "date=?", arrayOf(date), null, null, "id ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Record(
                        id = it.getLong(0),
                        date = it.getString(1),
                        type = it.getString(2),
                        memo = it.getString(3),
                        amount = it.getInt(4)
                    )
                )
            }
        }
        return list
    }
}

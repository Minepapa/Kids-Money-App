package com.minepapa.kidsmoneyapp

data class Record(
    val id: Long = 0,
    val date: String,
    val type: String,  // income, expense, tobank, frombank, direct_in, interest
    val memo: String,
    val amount: Int
) {
    val isIncome: Boolean
        get() = type == "income" || type == "frombank"

    val isExpense: Boolean
        get() = type == "expense"

    val isBank: Boolean
        get() = type == "tobank" || type == "direct_in" || type == "interest"
}

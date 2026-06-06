package com.example.tikstok.data.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Profile(
    val id: Long,
    name: String,
    val startingBalance: Double,
    val createdAt: Long = System.currentTimeMillis(),
) {
    var name by mutableStateOf(name)
        internal set

    var cash by mutableStateOf(startingBalance)
        internal set

    val holdings = mutableStateMapOf<String, Holding>()

    val transactions = mutableStateListOf<Transaction>()

    val cashEntries = mutableStateListOf<CashEntry>()

    init {
        cashEntries.add(CashEntry(CashFlowType.DEPOSIT, startingBalance, createdAt))
    }

    val netDeposits: Double
        get() = cashEntries.sumOf { if (it.type == CashFlowType.DEPOSIT) it.amount else -it.amount }
}

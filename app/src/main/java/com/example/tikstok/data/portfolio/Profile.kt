package com.example.tikstok.data.portfolio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * One isolated virtual portfolio under the account: its own cash, holdings, and full history. A user
 * can keep several of these and switch the active one from the Account screen. Session-scoped for now
 * — like [PortfolioStore], everything resets when the process is killed until Firestore/Room land.
 *
 * Mutations go through [PortfolioStore], which operates on the active profile; the fields here are
 * just the snapshot-backed state the UI reads.
 */
class Profile(
    val id: Long,
    name: String,
    val startingBalance: Double,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Display name shown on the Account screen and the top-bar profile chip. */
    var name by mutableStateOf(name)
        internal set

    /** Virtual cash available to spend, in USD. */
    var cash by mutableStateOf(startingBalance)
        internal set

    /** Open positions keyed by symbol. */
    val holdings = mutableStateMapOf<String, Holding>()

    /** Executed trades, oldest first. */
    val transactions = mutableStateListOf<Transaction>()

    /** Cash deposits and withdrawals, oldest first. */
    val cashEntries = mutableStateListOf<CashEntry>()

    init {
        // The starting balance counts as the first deposit so the cash history is complete.
        cashEntries.add(CashEntry(CashFlowType.DEPOSIT, startingBalance, createdAt))
    }

    /** Net money put in (deposits minus withdrawals) — the baseline for all-time P/L. */
    val netDeposits: Double
        get() = cashEntries.sumOf { if (it.type == CashFlowType.DEPOSIT) it.amount else -it.amount }
}

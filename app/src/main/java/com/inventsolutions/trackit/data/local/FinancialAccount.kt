package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "financial_accounts", indices = [Index("archived"), Index("createdAtEpochMillis")])
data class FinancialAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val openingBalanceMinor: Long,
    val openingDateEpochMillis: Long,
    val currency: String = "KES",
    val archived: Boolean = false,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val CASH = "CASH"
        const val MPESA = "MPESA"
        const val BANK = "BANK"
        const val OTHER = "OTHER"
        val TYPES = setOf(CASH, MPESA, BANK, OTHER)
    }
}

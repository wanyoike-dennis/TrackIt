package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_records",
    indices = [Index("type"), Index("createdAtEpochMillis")]
)
data class FinancialRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val targetMinor: Long,
    val currentMinor: Long = 0L,
    val dueDateEpochMillis: Long? = null,
    val notes: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEBT_OWED_TO_ME = "DEBT_OWED_TO_ME"
        const val DEBT_I_OWE = "DEBT_I_OWE"
        const val SAVINGS_GOAL = "SAVINGS_GOAL"
        const val ALLOWANCE = "ALLOWANCE"
    }
}

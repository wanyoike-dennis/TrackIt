package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets", indices = [Index(value = ["monthKey", "category"], unique = true)])
data class MonthlyBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val category: String,
    // ONE_TIME tracks the allocation as a single monthly pool; DAILY_ALLOWANCE also shows a daily guide.
    val budgetType: String = ONE_TIME,
    // KSh 1.00 is stored as 100 minor units.
    val limitMinor: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val ONE_TIME = "ONE_TIME"
        const val DAILY_ALLOWANCE = "DAILY_ALLOWANCE"
    }
}

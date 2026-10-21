package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets", indices = [Index(value = ["monthKey", "category"], unique = true)])
data class MonthlyBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthKey: String,
    val category: String,
    // KSh 1.00 is stored as 100 minor units.
    val limitMinor: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

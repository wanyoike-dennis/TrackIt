package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_categories",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String = name.trim().lowercase(),
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

package com.inventsolutions.trackit.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index("dateEpochMillis"), Index("type")]
)
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    // Store money as whole minor units to avoid floating-point rounding.
    val amountMinor: Long,
    val category: String,
    val description: String,
    val paymentMethod: String,
    val dateEpochMillis: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "NULL")
    val allowanceRecordId: Long? = null
) {
    companion object {
        const val INCOME = "INCOME"
        const val EXPENSE = "EXPENSE"
    }
}

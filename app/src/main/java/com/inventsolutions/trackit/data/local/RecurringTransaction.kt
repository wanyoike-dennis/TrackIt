package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [Index("nextRunEpochMillis"), Index("active")]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val amountMinor: Long,
    val category: String,
    val description: String = "",
    val paymentMethod: String = "Cash",
    val frequency: String,
    val nextRunEpochMillis: Long,
    val active: Boolean = true,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val WEEKLY = "WEEKLY"
        const val MONTHLY = "MONTHLY"
    }
}

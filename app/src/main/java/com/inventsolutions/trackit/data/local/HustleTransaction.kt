package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hustle_transactions",
    foreignKeys = [ForeignKey(entity = Hustle::class, parentColumns = ["id"], childColumns = ["hustleId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("hustleId"), Index("type"), Index("dateEpochMillis")]
)
data class HustleTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hustleId: Long,
    val type: String,
    val amountMinor: Long,
    val category: String = "",
    val description: String = "",
    val paymentMethod: String = "",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val INCOME = "INCOME"
        const val EXPENSE = "EXPENSE"
    }
}

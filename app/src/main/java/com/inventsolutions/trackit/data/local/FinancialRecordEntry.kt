package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_record_entries",
    foreignKeys = [
        ForeignKey(
            entity = FinancialRecord::class,
            parentColumns = ["id"],
            childColumns = ["financialRecordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("financialRecordId"), Index("createdAtEpochMillis")]
)
data class FinancialRecordEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val financialRecordId: Long,
    val entryType: String,
    val amountMinor: Long,
    val note: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val CONTRIBUTION = "CONTRIBUTION"
        const val REPAYMENT = "REPAYMENT"
        const val COLLECTION = "COLLECTION"
        const val SPENDING = "SPENDING"
        const val WITHDRAWAL = "WITHDRAWAL"
    }
}

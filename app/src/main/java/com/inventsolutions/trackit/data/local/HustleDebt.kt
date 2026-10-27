package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hustle_debts",
    foreignKeys = [ForeignKey(entity = Hustle::class, parentColumns = ["id"], childColumns = ["hustleId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("hustleId"), Index("direction"), Index("dueDateEpochMillis")]
)
data class HustleDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hustleId: Long,
    val direction: String,
    val counterparty: String,
    val description: String = "",
    val originalAmountMinor: Long,
    val dueDateEpochMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val CUSTOMER_OWES_ME = "CUSTOMER_OWES_ME"
        const val I_OWE = "I_OWE"
    }
}

package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hustle_debt_settlements",
    foreignKeys = [ForeignKey(entity = HustleDebt::class, parentColumns = ["id"], childColumns = ["debtId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("debtId"), Index("dateEpochMillis")]
)
data class HustleDebtSettlement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amountMinor: Long,
    val note: String = "",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

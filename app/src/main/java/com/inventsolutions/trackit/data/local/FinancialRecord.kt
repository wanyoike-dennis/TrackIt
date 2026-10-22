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
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val savingsCategory: String = "General savings",
    val contributionMethod: String = "FLEXIBLE",
    val plannedContributionMinor: Long = 0L
) {
    companion object {
        const val SAVINGS_EMERGENCY = "Emergency fund"
        const val SAVINGS_GOAL_BASED = "Goal-based savings"
        const val SAVINGS_UPCOMING_EXPENSES = "Bills and upcoming expenses"
        const val SAVINGS_LONG_TERM = "Long-term savings"
        const val SAVINGS_BUSINESS = "Business / hustle savings"
        const val SAVINGS_LIFESTYLE = "Event / lifestyle savings"
        const val SAVINGS_GENERAL = "General savings"

        const val CONTRIBUTION_WEEKLY = "WEEKLY"
        const val CONTRIBUTION_MONTHLY = "MONTHLY"
        const val CONTRIBUTION_FLEXIBLE = "FLEXIBLE"
        const val CONTRIBUTION_ONE_TIME = "ONE_TIME"
        const val DEBT_OWED_TO_ME = "DEBT_OWED_TO_ME"
        const val DEBT_I_OWE = "DEBT_I_OWE"
        const val SAVINGS_GOAL = "SAVINGS_GOAL"
        const val ALLOWANCE = "ALLOWANCE"
    }
}

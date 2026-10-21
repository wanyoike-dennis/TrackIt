package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE monthKey = :monthKey ORDER BY category COLLATE NOCASE")
    fun observeForMonth(monthKey: String): Flow<List<MonthlyBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(budget: MonthlyBudget): Long

    @Query("DELETE FROM monthly_budgets WHERE id = :budgetId")
    suspend fun deleteById(budgetId: Long)
}

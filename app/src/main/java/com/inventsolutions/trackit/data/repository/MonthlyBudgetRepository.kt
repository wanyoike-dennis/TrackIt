package com.inventsolutions.trackit.data.repository

import com.inventsolutions.trackit.data.local.MonthlyBudget
import com.inventsolutions.trackit.data.local.MonthlyBudgetDao
import kotlinx.coroutines.flow.Flow

class MonthlyBudgetRepository(private val dao: MonthlyBudgetDao) {
    fun observeForMonth(monthKey: String): Flow<List<MonthlyBudget>> = dao.observeForMonth(monthKey)
    suspend fun save(budget: MonthlyBudget) { dao.save(budget) }
    suspend fun delete(budgetId: Long) { dao.deleteById(budgetId) }
}

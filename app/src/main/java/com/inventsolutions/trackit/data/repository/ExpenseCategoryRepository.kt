package com.inventsolutions.trackit.data.repository

import com.inventsolutions.trackit.data.local.ExpenseCategory
import com.inventsolutions.trackit.data.local.ExpenseCategoryDao
import kotlinx.coroutines.flow.Flow

class ExpenseCategoryRepository(private val dao: ExpenseCategoryDao) {
    fun observeCategories(): Flow<List<ExpenseCategory>> = dao.observeAll()

    suspend fun ensureCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val normalized = trimmed.lowercase()
        if (dao.findByNormalizedName(normalized) == null) {
            dao.insert(ExpenseCategory(name = trimmed, normalizedName = normalized))
        }
    }
}

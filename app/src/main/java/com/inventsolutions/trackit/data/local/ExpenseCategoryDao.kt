package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExpenseCategory>>

    @Query("SELECT * FROM expense_categories WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): ExpenseCategory?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: ExpenseCategory): Long
}

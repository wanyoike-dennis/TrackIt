package com.inventsolutions.trackit.data.repository

import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.TransactionDao
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao
) {
    fun observeTransactions(): Flow<List<MoneyTransaction>> = dao.observeAll()

    suspend fun add(transaction: MoneyTransaction) {
        dao.insert(transaction)
    }

    suspend fun update(transaction: MoneyTransaction) {
        dao.update(transaction)
    }

    suspend fun delete(transactionId: Long) {
        dao.deleteById(transactionId)
    }
}

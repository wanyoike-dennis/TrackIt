package com.inventsolutions.trackit.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao
) {
    fun observeTransactions(): Flow<List<MoneyTransaction>> = dao.observeAll()

    suspend fun add(transaction: MoneyTransaction) {
        dao.insert(transaction)
    }
}

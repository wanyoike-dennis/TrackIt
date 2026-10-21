package com.inventsolutions.trackit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.TrackItDatabase
import com.inventsolutions.trackit.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionUiState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionRepository(
        TrackItDatabase.getInstance(application).transactionDao()
    )

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTransactions().collect { transactions ->
                _uiState.update { it.copy(transactions = transactions) }
            }
        }
    }

    suspend fun addTransaction(
        type: String,
        amountMinor: Long,
        category: String,
        description: String,
        paymentMethod: String,
        dateEpochMillis: Long
    ): Boolean {
        if (amountMinor <= 0L) {
            _uiState.update { it.copy(errorMessage = "Enter an amount greater than zero.") }
            return false
        }
        if (category.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Choose or enter a category.") }
            return false
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        return try {
            repository.add(
                MoneyTransaction(
                    type = type,
                    amountMinor = amountMinor,
                    category = category.trim(),
                    description = description.trim(),
                    paymentMethod = paymentMethod,
                    dateEpochMillis = dateEpochMillis
                )
            )
            _uiState.update { it.copy(isSaving = false, errorMessage = null) }
            true
        } catch (_: Exception) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    errorMessage = "Couldn't save this transaction. Please try again."
                )
            }
            false
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

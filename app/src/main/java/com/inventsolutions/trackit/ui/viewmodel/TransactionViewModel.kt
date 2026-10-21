package com.inventsolutions.trackit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.MonthlyBudget
import com.inventsolutions.trackit.data.local.TrackItDatabase
import com.inventsolutions.trackit.data.repository.MonthlyBudgetRepository
import com.inventsolutions.trackit.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionUiState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val budgets: List<MonthlyBudget> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TrackItDatabase.getInstance(application)
    private val transactionRepository = TransactionRepository(database.transactionDao())
    private val budgetRepository = MonthlyBudgetRepository(database.monthlyBudgetDao())
    private val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.observeTransactions().collect { transactions ->
                _uiState.update { it.copy(transactions = transactions) }
            }
        }
        viewModelScope.launch {
            budgetRepository.observeForMonth(monthKey).collect { budgets ->
                _uiState.update { it.copy(budgets = budgets) }
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
            transactionRepository.add(
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
                it.copy(isSaving = false, errorMessage = "Couldn't save this transaction. Please try again.")
            }
            false
        }
    }

    fun saveBudget(category: String, limitMinor: Long) {
        if (category.isBlank() || limitMinor <= 0L) return
        viewModelScope.launch {
            try {
                budgetRepository.save(
                    MonthlyBudget(
                        monthKey = monthKey,
                        category = category.trim(),
                        limitMinor = limitMinor
                    )
                )
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't save this budget. Please try again.") }
            }
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            try {
                budgetRepository.delete(budgetId)
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't delete this budget. Please try again.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

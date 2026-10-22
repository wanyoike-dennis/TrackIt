package com.inventsolutions.trackit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inventsolutions.trackit.data.local.ExpenseCategory
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.MonthlyBudget
import com.inventsolutions.trackit.data.local.TrackItDatabase
import com.inventsolutions.trackit.data.repository.ExpenseCategoryRepository
import com.inventsolutions.trackit.data.repository.MonthlyBudgetRepository
import com.inventsolutions.trackit.data.repository.TransactionRepository
import com.inventsolutions.trackit.notifications.BudgetNotifications
import java.text.NumberFormat
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
    val expenseCategories: List<ExpenseCategory> = emptyList(),
    val financialRecords: List<FinancialRecord> = emptyList(),
    val financialEntries: List<FinancialRecordEntry> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val database = TrackItDatabase.getInstance(application)
    private val transactionRepository = TransactionRepository(database.transactionDao())
    private val budgetRepository = MonthlyBudgetRepository(database.monthlyBudgetDao())
    private val categoryRepository = ExpenseCategoryRepository(database.expenseCategoryDao())
    private val financialRecordDao = database.financialRecordDao()
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
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(expenseCategories = categories) }
            }
        }
        viewModelScope.launch {
            financialRecordDao.observeAll().collect { records ->
                _uiState.update { it.copy(financialRecords = records) }
            }
        }
        viewModelScope.launch {
            financialRecordDao.observeAllEntries().collect { entries ->
                _uiState.update { it.copy(financialEntries = entries) }
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
            val normalizedCategory = category.trim()
            if (type == MoneyTransaction.EXPENSE) {
                categoryRepository.ensureCategory(normalizedCategory)
            }
            val transaction = MoneyTransaction(
                type = type,
                amountMinor = amountMinor,
                category = normalizedCategory,
                description = description.trim(),
                paymentMethod = paymentMethod,
                dateEpochMillis = dateEpochMillis
            )
            val stateBeforeSave = _uiState.value
            transactionRepository.add(transaction)
            if (type == MoneyTransaction.EXPENSE) {
                notifyIfBudgetExceeded(transaction, stateBeforeSave.transactions, stateBeforeSave.budgets)
            }
            _uiState.update { it.copy(isSaving = false, errorMessage = null) }
            true
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isSaving = false, errorMessage = "Couldn't save this transaction. Please try again.")
            }
            false
        }
    }

    fun createExpenseCategory(name: String, budgetLimitMinor: Long?, budgetType: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a category name.") }
            return
        }
        if (budgetLimitMinor != null && budgetLimitMinor <= 0L) {
            _uiState.update { it.copy(errorMessage = "Enter a valid budget amount greater than zero.") }
            return
        }
        viewModelScope.launch {
            try {
                categoryRepository.ensureCategory(trimmed)
                if (budgetLimitMinor != null) {
                    budgetRepository.save(
                        MonthlyBudget(
                            monthKey = monthKey,
                            category = trimmed,
                            budgetType = budgetType,
                            limitMinor = budgetLimitMinor
                        )
                    )
                }
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't create this category. Please try again.") }
            }
        }
    }

    private fun notifyIfBudgetExceeded(
        added: MoneyTransaction,
        previousTransactions: List<MoneyTransaction>,
        budgets: List<MonthlyBudget>
    ) {
        val transactionMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(added.dateEpochMillis))
        val transactionDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(added.dateEpochMillis))
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (transactionMonth != monthKey) return

        val daysInMonth = java.util.Calendar.getInstance().apply {
            timeInMillis = added.dateEpochMillis
        }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        budgets.forEach { budget ->
            if (!budget.category.equals(added.category, ignoreCase = true)) return@forEach

            val previousMonthSpend = previousTransactions
                .filter {
                    it.type == MoneyTransaction.EXPENSE &&
                        it.category.equals(budget.category, ignoreCase = true) &&
                        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.dateEpochMillis)) == transactionMonth
                }
                .sumOf { it.amountMinor }
            val currentMonthSpend = previousMonthSpend + added.amountMinor

            if (previousMonthSpend <= budget.limitMinor && currentMonthSpend > budget.limitMinor) {
                BudgetNotifications.show(
                    app,
                    "Monthly budget exceeded",
                    "${budget.category}: you've gone ${formatKes(currentMonthSpend - budget.limitMinor)} over your monthly limit.",
                    notificationId(budget.id, "monthly")
                )
            }

            if (budget.budgetType == MonthlyBudget.RECURRING && transactionDay == todayKey) {
                val previousTodaySpend = previousTransactions
                    .filter {
                        it.type == MoneyTransaction.EXPENSE &&
                            it.category.equals(budget.category, ignoreCase = true) &&
                            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.dateEpochMillis)) == todayKey
                    }
                    .sumOf { it.amountMinor }
                val dailyLimit = budget.limitMinor / daysInMonth
                val currentTodaySpend = previousTodaySpend + added.amountMinor

                if (previousTodaySpend <= dailyLimit && currentTodaySpend > dailyLimit) {
                    BudgetNotifications.show(
                        app,
                        "Recurring budget exceeded",
                        "${budget.category}: you've gone ${formatKes(currentTodaySpend - dailyLimit)} over today's recurring budget.",
                        notificationId(budget.id, "recurring")
                    )
                }
            }
        }
    }

    private fun notificationId(budgetId: Long, type: String): Int =
        (budgetId * 31L + type.hashCode()).toInt() and 0x7fffffff

    private fun formatKes(minor: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = if (minor % 100L == 0L) 0 else 2
        }
        return "KSh " + formatter.format(minor / 100.0)
    }

    fun updateTransaction(transaction: MoneyTransaction) {
        if (transaction.amountMinor <= 0L || transaction.category.isBlank() || transaction.paymentMethod.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Check the amount, category, and payment method.") }
            return
        }
        viewModelScope.launch {
            try {
                val normalized = transaction.copy(
                    category = transaction.category.trim(),
                    description = transaction.description.trim(),
                    paymentMethod = transaction.paymentMethod.trim()
                )
                if (normalized.type == MoneyTransaction.EXPENSE) {
                    categoryRepository.ensureCategory(normalized.category)
                }
                transactionRepository.update(normalized)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't update this transaction. Please try again.") }
            }
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch {
            try {
                transactionRepository.delete(transactionId)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't delete this transaction. Please try again.") }
            }
        }
    }

    fun saveBudget(category: String, limitMinor: Long, budgetType: String) {
        if (category.isBlank() || limitMinor <= 0L) return
        viewModelScope.launch {
            try {
                categoryRepository.ensureCategory(category)
                budgetRepository.save(
                    MonthlyBudget(
                        monthKey = monthKey,
                        category = category.trim(),
                        budgetType = budgetType,
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

    fun addFinancialRecord(
        type: String,
        title: String,
        targetMinor: Long,
        dueDateEpochMillis: Long? = null,
        notes: String = ""
    ) {
        if (type !in setOf(
                FinancialRecord.DEBT_OWED_TO_ME,
                FinancialRecord.DEBT_I_OWE,
                FinancialRecord.SAVINGS_GOAL,
                FinancialRecord.ALLOWANCE
            ) || title.isBlank() || targetMinor <= 0L
        ) {
            _uiState.update { it.copy(errorMessage = "Enter a title and an amount greater than zero.") }
            return
        }
        viewModelScope.launch {
            try {
                financialRecordDao.insert(
                    FinancialRecord(
                        type = type,
                        title = title.trim(),
                        targetMinor = targetMinor,
                        dueDateEpochMillis = dueDateEpochMillis,
                        notes = notes.trim()
                    )
                )
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't save this record. Please try again.") }
            }
        }
    }

    fun updateFinancialRecord(record: FinancialRecord) {
        if (record.title.isBlank() || record.targetMinor <= 0L || record.currentMinor < 0L) {
            _uiState.update { it.copy(errorMessage = "Enter a title and a valid amount greater than zero.") }
            return
        }
        viewModelScope.launch {
            try {
                financialRecordDao.update(record.copy(title = record.title.trim(), notes = record.notes.trim()))
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't edit this plan. Please try again.") }
            }
        }
    }

    fun addFinancialProgress(record: FinancialRecord, amountMinor: Long, note: String = "") {
        if (amountMinor <= 0L) {
            _uiState.update { it.copy(errorMessage = "Enter an amount greater than zero.") }
            return
        }
        val isDebt = record.type == FinancialRecord.DEBT_I_OWE ||
            record.type == FinancialRecord.DEBT_OWED_TO_ME
        val outstanding = (record.targetMinor - record.currentMinor).coerceAtLeast(0L)
        if (isDebt && amountMinor > outstanding) {
            _uiState.update { it.copy(errorMessage = "That amount is more than the outstanding debt.") }
            return
        }
        val entryType = when (record.type) {
            FinancialRecord.DEBT_I_OWE -> FinancialRecordEntry.REPAYMENT
            FinancialRecord.DEBT_OWED_TO_ME -> FinancialRecordEntry.COLLECTION
            FinancialRecord.SAVINGS_GOAL -> FinancialRecordEntry.CONTRIBUTION
            else -> FinancialRecordEntry.SPENDING
        }
        viewModelScope.launch {
            try {
                financialRecordDao.saveProgress(
                    record.copy(currentMinor = record.currentMinor + amountMinor),
                    FinancialRecordEntry(
                        financialRecordId = record.id,
                        entryType = entryType,
                        amountMinor = amountMinor,
                        note = note.trim()
                    )
                )
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't save this update. Please try again.") }
            }
        }
    }

    fun deleteFinancialRecord(recordId: Long) {
        viewModelScope.launch {
            try {
                financialRecordDao.deleteById(recordId)
                _uiState.update { it.copy(errorMessage = null) }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Couldn't delete this record. Please try again.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

package com.inventsolutions.trackit.ui.screens.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.ExpenseCategory
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.MonthlyBudget
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val paymentMethods = listOf("Cash", "M-Pesa", "Bank", "Card", "Other")

private fun parseMinorUnits(value: String): Long? = runCatching {
    BigDecimal(value.trim().replace(",", ""))
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()

private fun formatKes(minor: Long): String {
    val formatter = java.text.NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (minor % 100L == 0L) 0 else 2
    }
    return "KSh " + formatter.format(minor / 100.0)
}

@Composable
fun TransactionEntryScreen(
    type: String,
    isSaving: Boolean,
    errorMessage: String?,
    budgets: List<MonthlyBudget>,
    transactions: List<MoneyTransaction>,
    expenseCategories: List<ExpenseCategory>,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onCreateCategory: (name: String, budgetLimitMinor: Long?, budgetType: String) -> Unit,
    onSave: (
        type: String,
        amountMinor: Long,
        category: String,
        description: String,
        paymentMethod: String,
        dateEpochMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("M-Pesa") }
    var paymentMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var createCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var createBudgetNow by remember { mutableStateOf(false) }
    var newBudgetAmount by remember { mutableStateOf("") }
    var newBudgetType by remember { mutableStateOf(MonthlyBudget.ONE_TIME) }
    var categoryDialogError by remember { mutableStateOf<String?>(null) }
    var dateEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val isIncome = type == MoneyTransaction.INCOME
    val dateLabel = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(dateEpochMillis))
    val categoryNames = (expenseCategories.map { it.name } + budgets.map { it.category })
        .distinctBy { it.trim().lowercase(Locale.getDefault()) }
        .sortedBy { it.lowercase(Locale.getDefault()) }
    val selectedDate = Calendar.getInstance().apply { timeInMillis = dateEpochMillis }
    val selectedMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(dateEpochMillis))
    val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val selectedBudget = if (isIncome || selectedMonthKey != currentMonthKey) null else budgets.firstOrNull {
        it.category.equals(category, ignoreCase = true)
    }
    val selectedDayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(dateEpochMillis))
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val monthSpend = if (selectedBudget != null && selectedMonthKey == currentMonthKey) {
        transactions.filter {
            it.type == MoneyTransaction.EXPENSE &&
                it.category.equals(category, ignoreCase = true) &&
                SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.dateEpochMillis)) == selectedMonthKey
        }.sumOf { it.amountMinor }
    } else 0L
    val enteredMinor = parseMinorUnits(amount) ?: 0L
    val dailySpend = if (
        selectedBudget?.budgetType == MonthlyBudget.RECURRING &&
        selectedDayKey == todayKey
    ) {
        transactions.filter {
            it.type == MoneyTransaction.EXPENSE &&
                it.category.equals(category, ignoreCase = true) &&
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.dateEpochMillis)) == todayKey
        }.sumOf { it.amountMinor }
    } else 0L
    val daysInSelectedMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dailyLimit = selectedBudget?.limitMinor?.div(daysInSelectedMonth.toLong()) ?: 0L
    val monthAfterExpense = monthSpend + enteredMinor
    val dailyAfterExpense = dailySpend + enteredMinor

    if (createCategoryDialog && !isIncome) {
        AlertDialog(
            onDismissRequest = { createCategoryDialog = false; categoryDialogError = null },
            title = { Text("Create expense category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it; categoryDialogError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category name") },
                        placeholder = { Text("e.g. Medical, Internet") },
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = createBudgetNow, onCheckedChange = {
                            createBudgetNow = it
                            categoryDialogError = null
                        })
                        Column {
                            Text("Set a budget now", fontWeight = FontWeight.Medium)
                            Text(
                                "You can also add a budget later.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (createBudgetNow) {
                        OutlinedTextField(
                            value = newBudgetAmount,
                            onValueChange = { newBudgetAmount = it; categoryDialogError = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Monthly allocation (KSh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = newBudgetType == MonthlyBudget.ONE_TIME,
                                onClick = { newBudgetType = MonthlyBudget.ONE_TIME }
                            )
                            Text("One-time")
                            Spacer(Modifier.padding(horizontal = 5.dp))
                            RadioButton(
                                selected = newBudgetType == MonthlyBudget.RECURRING,
                                onClick = { newBudgetType = MonthlyBudget.RECURRING }
                            )
                            Text("Recurring")
                        }
                        Text(
                            if (newBudgetType == MonthlyBudget.RECURRING)
                                "The monthly allocation becomes a daily guide. Unused daily allowance does not roll over."
                            else "Track spending against the full monthly allocation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    categoryDialogError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val name = newCategoryName.trim()
                    val duplicate = categoryNames.any { it.equals(name, ignoreCase = true) }
                    val limit = if (createBudgetNow) parseMinorUnits(newBudgetAmount) else null
                    when {
                        name.isBlank() -> categoryDialogError = "Enter a category name."
                        duplicate -> categoryDialogError = "This category already exists. Choose it from the list."
                        createBudgetNow && (limit == null || limit <= 0L) ->
                            categoryDialogError = "Enter a valid budget amount greater than zero."
                        else -> {
                            onCreateCategory(name, limit, newBudgetType)
                            category = name
                            createCategoryDialog = false
                            newCategoryName = ""
                            newBudgetAmount = ""
                            createBudgetNow = false
                            newBudgetType = MonthlyBudget.ONE_TIME
                            categoryDialogError = null
                        }
                    }
                }) { Text("Create category") }
            },
            dismissButton = {
                OutlinedButton(onClick = { createCategoryDialog = false; categoryDialogError = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isIncome) "Add income" else "Add expense",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isIncome) "Record money you received." else "Record money you spent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onBack) { Text("Cancel") }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (isIncome) "INCOME AMOUNT" else "EXPENSE AMOUNT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(12)
                        if (errorMessage != null) onClearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (KSh)") },
                    placeholder = { Text("e.g. 2500") },
                    prefix = { Text("KSh ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                if (isIncome) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it; if (errorMessage != null) onClearError() },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category") },
                        placeholder = { Text("e.g. Side hustle, Salary") },
                        singleLine = true
                    )
                    Text("Quick categories", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Salary", "Side hustle", "Gift").forEach { suggestion ->
                            OutlinedButton(onClick = { category = suggestion }) { Text(suggestion) }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Budget category", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            onClick = { categoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.ifBlank { "Choose a category" }, modifier = Modifier.weight(1f))
                            Text("⌄")
                        }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            categoryNames.forEach { name ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(name)
                                            val budget = budgets.firstOrNull { it.category.equals(name, ignoreCase = true) }
                                            if (budget != null) {
                                                Text(
                                                    "Budget: ${formatKes(budget.limitMinor)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text("No budget set", style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    },
                                    onClick = {
                                        category = name
                                        categoryMenuExpanded = false
                                        if (errorMessage != null) onClearError()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Create new category") },
                                onClick = {
                                    categoryMenuExpanded = false
                                    newCategoryName = ""
                                    newBudgetAmount = ""
                                    createBudgetNow = false
                                    categoryDialogError = null
                                    createCategoryDialog = true
                                }
                            )
                        }
                        if (selectedBudget != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Budget status", style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text("Monthly allocation: ${formatKes(selectedBudget.limitMinor)}")
                                    Text("Spent this month: ${formatKes(monthSpend)}")
                                    Text(
                                        if (selectedBudget.limitMinor - monthSpend >= 0L)
                                            "Remaining before expense: ${formatKes(selectedBudget.limitMinor - monthSpend)}"
                                        else "Already over budget by ${formatKes(monthSpend - selectedBudget.limitMinor)}",
                                        color = if (monthSpend > selectedBudget.limitMinor)
                                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (enteredMinor > 0L && monthAfterExpense > selectedBudget.limitMinor) {
                                        Text(
                                            "Warning: this expense will put the monthly budget ${formatKes(monthAfterExpense - selectedBudget.limitMinor)} over its limit. You can still save it.",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (selectedBudget.budgetType == MonthlyBudget.RECURRING) {
                                        Text("Daily guide: ${formatKes(dailyLimit)}")
                                        if (selectedDayKey == todayKey && enteredMinor > 0L && dailyAfterExpense > dailyLimit) {
                                            Text(
                                                "Warning: this will exceed today's recurring guide by ${formatKes(dailyAfterExpense - dailyLimit)}. You can still save it.",
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (category.isNotBlank()) {
                            Text("No budget is set for this category. You can still record the expense.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What was this for?") },
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Date", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { timeInMillis = dateEpochMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    dateEpochMillis = chosen.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(dateLabel) }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Payment method", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { paymentMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(paymentMethod)
                    }
                    DropdownMenu(expanded = paymentMenuExpanded, onDismissRequest = { paymentMenuExpanded = false }) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(text = { Text(method) }, onClick = {
                                paymentMethod = method
                                paymentMenuExpanded = false
                            })
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                onSave(
                    type,
                    parseMinorUnits(amount) ?: 0L,
                    category,
                    description,
                    paymentMethod,
                    dateEpochMillis
                )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save " + if (isIncome) "income" else "expense")
            }
        }
        Text("Your transaction is saved on this device and works offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

package com.inventsolutions.trackit.ui.screens.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class TransactionTypeFilter { ALL, INCOME, EXPENSE }
private enum class TransactionPeriodFilter { ALL_TIME, THIS_MONTH }

private fun parseHistoryAmount(value: String): Long? = runCatching {
    BigDecimal(value.trim().replace(",", ""))
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}.getOrNull()?.takeIf { it > 0L }

private fun historyKes(amountMinor: Long): String {
    val absolute = abs(amountMinor)
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (absolute % 100L == 0L) 0 else 2
    }
    return "KSh " + formatter.format(absolute / 100.0)
}

@Composable
fun TransactionHistoryScreen(
    transactions: List<MoneyTransaction>,
    errorMessage: String?,
    onBack: () -> Unit,
    onUpdate: (MoneyTransaction) -> Unit,
    onDelete: (Long) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var periodFilter by remember { mutableStateOf(TransactionPeriodFilter.ALL_TIME) }
    var selectedTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var editingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deleteCandidate by remember { mutableStateOf<MoneyTransaction?>(null) }

    val now = Calendar.getInstance()
    val currentMonthKey = "%04d-%02d".format(Locale.US, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    val filtered = transactions.filter { transaction ->
        val matchesType = when (typeFilter) {
            TransactionTypeFilter.ALL -> true
            TransactionTypeFilter.INCOME -> transaction.type == MoneyTransaction.INCOME
            TransactionTypeFilter.EXPENSE -> transaction.type == MoneyTransaction.EXPENSE
        }
        val transactionDate = Calendar.getInstance().apply { timeInMillis = transaction.dateEpochMillis }
        val transactionMonthKey = "%04d-%02d".format(
            Locale.US, transactionDate.get(Calendar.YEAR), transactionDate.get(Calendar.MONTH) + 1
        )
        val matchesPeriod = periodFilter == TransactionPeriodFilter.ALL_TIME || transactionMonthKey == currentMonthKey
        val query = search.trim()
        val matchesSearch = query.isBlank() ||
            transaction.category.contains(query, ignoreCase = true) ||
            transaction.description.contains(query, ignoreCase = true) ||
            transaction.paymentMethod.contains(query, ignoreCase = true)
        matchesType && matchesPeriod && matchesSearch
    }
    val filteredIncome = filtered.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor }
    val filteredExpenses = filtered.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Transaction history", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Search, review, edit or remove a record.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("Home") }
        }

        OutlinedTextField(
            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Search transactions") }, placeholder = { Text("Category, description, payment method") },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = typeFilter == TransactionTypeFilter.ALL,
                onClick = { typeFilter = TransactionTypeFilter.ALL }, label = { Text("All") })
            FilterChip(selected = typeFilter == TransactionTypeFilter.INCOME,
                onClick = { typeFilter = TransactionTypeFilter.INCOME }, label = { Text("Income") })
            FilterChip(selected = typeFilter == TransactionTypeFilter.EXPENSE,
                onClick = { typeFilter = TransactionTypeFilter.EXPENSE }, label = { Text("Expenses") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = periodFilter == TransactionPeriodFilter.ALL_TIME,
                onClick = { periodFilter = TransactionPeriodFilter.ALL_TIME }, label = { Text("All time") })
            FilterChip(selected = periodFilter == TransactionPeriodFilter.THIS_MONTH,
                onClick = { periodFilter = TransactionPeriodFilter.THIS_MONTH }, label = { Text("This month") })
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(historyKes(filteredIncome), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(historyKes(filteredExpenses), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (filtered.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No transactions found", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (transactions.isEmpty()) "Your saved income and expenses will appear here."
                        else "Try changing the search or filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { transaction ->
                    val isIncome = transaction.type == MoneyTransaction.INCOME
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedTransaction = transaction },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(transaction.description.ifBlank { transaction.category },
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${transaction.category} · ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(transaction.dateEpochMillis))}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(transaction.paymentMethod, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text((if (isIncome) "+ " else "− ") + historyKes(transaction.amountMinor),
                                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("View details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { selectedTransaction = null },
            title = { Text(if (transaction.type == MoneyTransaction.INCOME) "Income details" else "Expense details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(transaction.description.ifBlank { transaction.category }, style = MaterialTheme.typography.titleMedium)
                    Text("Amount: ${historyKes(transaction.amountMinor)}")
                    Text("Category: ${transaction.category}")
                    Text("Payment: ${transaction.paymentMethod}")
                    Text("Date: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(transaction.dateEpochMillis))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { editingTransaction = transaction; selectedTransaction = null }) { Text("Edit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { deleteCandidate = transaction; selectedTransaction = null }) { Text("Delete") }
                    TextButton(onClick = { selectedTransaction = null }) { Text("Close") }
                }
            }
        )
    }

    editingTransaction?.let { transaction ->
        var amount by remember(transaction.id) { mutableStateOf("%.2f".format(Locale.US, transaction.amountMinor / 100.0)) }
        var category by remember(transaction.id) { mutableStateOf(transaction.category) }
        var description by remember(transaction.id) { mutableStateOf(transaction.description) }
        var paymentMethod by remember(transaction.id) { mutableStateOf(transaction.paymentMethod) }
        var validationError by remember(transaction.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingTransaction = null },
            title = { Text("Edit transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(12); validationError = null },
                        label = { Text("Amount (KSh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it; validationError = null },
                        label = { Text("Category") }, singleLine = true)
                    OutlinedTextField(value = description, onValueChange = { description = it },
                        label = { Text("Description (optional)") }, singleLine = true)
                    OutlinedTextField(value = paymentMethod, onValueChange = { paymentMethod = it },
                        label = { Text("Payment method") }, singleLine = true)
                    validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amountMinor = parseHistoryAmount(amount)
                    if (amountMinor == null) validationError = "Enter an amount greater than zero."
                    else if (category.isBlank()) validationError = "Enter a category."
                    else if (paymentMethod.isBlank()) validationError = "Enter a payment method."
                    else {
                        onUpdate(transaction.copy(amountMinor = amountMinor, category = category.trim(),
                            description = description.trim(), paymentMethod = paymentMethod.trim()))
                        editingTransaction = null
                    }
                }) { Text("Save changes") }
            },
            dismissButton = { TextButton(onClick = { editingTransaction = null }) { Text("Cancel") } }
        )
    }

    deleteCandidate?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete transaction?") },
            text = { Text("This will permanently remove this ${if (transaction.type == MoneyTransaction.INCOME) "income" else "expense"} record from this device.") },
            confirmButton = {
                Button(onClick = { onDelete(transaction.id); deleteCandidate = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } }
        )
    }
}

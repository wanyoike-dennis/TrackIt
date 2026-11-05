package com.inventsolutions.trackit.ui.screens.transactions

import com.inventsolutions.trackit.R
import com.inventsolutions.trackit.ui.components.TrackItIcon
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.AccountTransfer
import com.inventsolutions.trackit.data.local.FinancialAccount
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class TransactionTypeFilter { ALL, INCOME, EXPENSE, TRANSFERS, SAVINGS, DEBT }
private enum class ActivityKind { INCOME, EXPENSE, TRANSFER, SAVINGS, DEBT }
private data class ActivityEvent(val key: String, val kind: ActivityKind, val title: String, val subtitle: String, val amountMinor: Long, val dateMillis: Long, val positive: Boolean = false, val transaction: MoneyTransaction? = null)
private enum class TransactionPeriodFilter { ALL_TIME, THIS_MONTH, CUSTOM }
private enum class ActivityDatePickerTarget { START, END }

private fun todayUtcDatePickerMillis(): Long {
    val localToday = Calendar.getInstance()
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(localToday.get(Calendar.YEAR), localToday.get(Calendar.MONTH), localToday.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun localDayStartFromUtcDatePicker(millis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

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
    financialRecords: List<FinancialRecord>,
    financialEntries: List<FinancialRecordEntry>,
    accountTransfers: List<AccountTransfer>,
    accounts: List<FinancialAccount>,
    errorMessage: String?,
    onBack: () -> Unit,
    onUpdate: (MoneyTransaction) -> Unit,
    onDelete: (Long) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var periodFilter by remember { mutableStateOf(TransactionPeriodFilter.ALL_TIME) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var datePickerTarget by remember { mutableStateOf<ActivityDatePickerTarget?>(null) }
    var dateRangeError by remember { mutableStateOf<String?>(null) }
    val now = Calendar.getInstance()
    val monthKeyNow = "%04d-%02d".format(Locale.US, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    val accountNames = accounts.associate { it.id to it.name }
    val allEvents = buildList {
        transactions.forEach { tx ->
            val kind = if (tx.type == MoneyTransaction.INCOME) ActivityKind.INCOME else ActivityKind.EXPENSE
            add(ActivityEvent("tx-${tx.id}", kind, tx.description.ifBlank { tx.category },
                listOf(tx.category, tx.paymentMethod).filter { it.isNotBlank() }.joinToString(" · "), tx.amountMinor, tx.dateEpochMillis, positive = kind == ActivityKind.INCOME, transaction = tx))
        }
        accountTransfers.forEach { transfer ->
            val from = accountNames[transfer.fromAccountId] ?: "Account"
            val to = accountNames[transfer.toAccountId] ?: "Account"
            add(ActivityEvent("transfer-${transfer.id}", ActivityKind.TRANSFER, transfer.note.ifBlank { "Account transfer" },
                "$from → $to", transfer.amountMinor, transfer.dateEpochMillis))
        }
        financialEntries.forEach { entry ->
            val record = financialRecords.firstOrNull { it.id == entry.financialRecordId } ?: return@forEach
            val kind = when {
                record.type in setOf(FinancialRecord.SAVINGS_GOAL, FinancialRecord.SAVINGS_ACCOUNT) && entry.entryType in setOf(FinancialRecordEntry.CONTRIBUTION, FinancialRecordEntry.WITHDRAWAL) -> ActivityKind.SAVINGS
                record.type in setOf(FinancialRecord.DEBT_I_OWE, FinancialRecord.DEBT_OWED_TO_ME) && entry.entryType in setOf(FinancialRecordEntry.REPAYMENT, FinancialRecordEntry.COLLECTION) -> ActivityKind.DEBT
                else -> null
            } ?: return@forEach
            val action = when (entry.entryType) {
                FinancialRecordEntry.CONTRIBUTION -> "Savings contribution"
                FinancialRecordEntry.WITHDRAWAL -> "Savings withdrawal"
                FinancialRecordEntry.REPAYMENT -> "Debt repayment"
                FinancialRecordEntry.COLLECTION -> "Debt collection"
                else -> "Financial update"
            }
            val accountLabel = entry.accountId?.let { accountNames[it] }
            val detail = listOf(record.title, accountLabel).filterNotNull().joinToString(" · ")
            add(ActivityEvent("entry-${entry.id}", kind, entry.note.ifBlank { action }, detail, entry.amountMinor, entry.createdAtEpochMillis, positive = entry.entryType == FinancialRecordEntry.WITHDRAWAL || entry.entryType == FinancialRecordEntry.COLLECTION))
        }
    }.sortedByDescending { it.dateMillis }
    val filtered = allEvents.filter { event ->
        val matchesType = when (typeFilter) {
            TransactionTypeFilter.ALL -> true
            TransactionTypeFilter.INCOME -> event.kind == ActivityKind.INCOME
            TransactionTypeFilter.EXPENSE -> event.kind == ActivityKind.EXPENSE
            TransactionTypeFilter.TRANSFERS -> event.kind == ActivityKind.TRANSFER
            TransactionTypeFilter.SAVINGS -> event.kind == ActivityKind.SAVINGS
            TransactionTypeFilter.DEBT -> event.kind == ActivityKind.DEBT
        }
        val date = Calendar.getInstance().apply { timeInMillis = event.dateMillis }
        val monthKey = "%04d-%02d".format(Locale.US, date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1)
        val matchesPeriod = when (periodFilter) {
            TransactionPeriodFilter.ALL_TIME -> true
            TransactionPeriodFilter.THIS_MONTH -> monthKey == monthKeyNow
            TransactionPeriodFilter.CUSTOM -> {
                val start = customStartDate?.let(::localDayStartFromUtcDatePicker)
                val endExclusive = customEndDate?.let(::localDayStartFromUtcDatePicker)?.let { day ->
                    Calendar.getInstance().apply { timeInMillis = day; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
                }
                start != null && endExclusive != null && event.dateMillis >= start && event.dateMillis < endExclusive
            }
        }
        val query = search.trim()
        val matchesSearch = query.isBlank() || event.title.contains(query, true) || event.subtitle.contains(query, true) ||
            event.transaction?.category?.contains(query, true) == true || event.transaction?.paymentMethod?.contains(query, true) == true
        matchesType && matchesPeriod && matchesSearch
    }
    val filteredIncome = filtered.filter { it.kind == ActivityKind.INCOME }.sumOf { it.amountMinor }
    val filteredExpenses = filtered.filter { it.kind == ActivityKind.EXPENSE }.sumOf { it.amountMinor }
    val filteredNet = filteredIncome - filteredExpenses
    var selectedTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var selectedEvent by remember { mutableStateOf<ActivityEvent?>(null) }
    var editingTransaction by remember { mutableStateOf<MoneyTransaction?>(null) }
    var deleteCandidate by remember { mutableStateOf<MoneyTransaction?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Activity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your complete money movement timeline.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Search transactions") }, leadingIcon = { TrackItIcon(R.drawable.ic_lucide_search, null) }, placeholder = { Text("Description, category, account or record") },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(TransactionTypeFilter.ALL to "All", TransactionTypeFilter.INCOME to "Income", TransactionTypeFilter.EXPENSE to "Expenses").forEach { (filter, label) ->
                FilterChip(selected = typeFilter == filter, onClick = { typeFilter = filter }, label = { Text(label) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(TransactionTypeFilter.TRANSFERS to "Transfers", TransactionTypeFilter.SAVINGS to "Savings", TransactionTypeFilter.DEBT to "Debt").forEach { (filter, label) ->
                FilterChip(selected = typeFilter == filter, onClick = { typeFilter = filter }, label = { Text(label) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = periodFilter == TransactionPeriodFilter.ALL_TIME,
                onClick = { periodFilter = TransactionPeriodFilter.ALL_TIME }, label = { Text("All time") })
            FilterChip(selected = periodFilter == TransactionPeriodFilter.THIS_MONTH,
                onClick = { periodFilter = TransactionPeriodFilter.THIS_MONTH }, label = { Text("This month") })
            FilterChip(selected = periodFilter == TransactionPeriodFilter.CUSTOM,
                onClick = { dateRangeError = null; datePickerTarget = ActivityDatePickerTarget.START },
                label = { Text(if (periodFilter == TransactionPeriodFilter.CUSTOM && customStartDate != null && customEndDate != null) "Custom dates" else "Date range") })
        }
        if (periodFilter == TransactionPeriodFilter.CUSTOM && customStartDate != null && customEndDate != null) {
            Text(
                "${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(customStartDate!!))} – ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(customEndDate!!))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(historyKes(filteredIncome), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(historyKes(filteredExpenses), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                }
                Column(Modifier.weight(1f)) {
                    Text("Net", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text((if (filteredNet >= 0) "+ " else "− ") + historyKes(filteredNet), style = MaterialTheme.typography.titleSmall,
                        color = if (filteredNet >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                }
            }
        }

        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (filtered.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No activity found", style = MaterialTheme.typography.titleMedium)
                    Text(if (allEvents.isEmpty()) "Your income, expenses, transfers, savings and debt updates will appear here." else "Try changing the search or filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.groupBy { DateFormat.getDateInstance(DateFormat.FULL).format(Date(it.dateMillis)) }.forEach { (dateLabel, events) ->
                    item(key = "date-$dateLabel") {
                        Text(dateLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    }
                    items(events, key = { it.key }) { event ->
                        val positive = event.positive
                        val amount = if (event.kind == ActivityKind.TRANSFER) historyKes(event.amountMinor) else (if (positive) "+ " else "− ") + historyKes(event.amountMinor)
                        Card(modifier = Modifier.fillMaxWidth().clickable { if (event.transaction != null) selectedTransaction = event.transaction else selectedEvent = event },
                            shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(event.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(event.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(when (event.kind) { ActivityKind.INCOME -> "Income"; ActivityKind.EXPENSE -> "Expense"; ActivityKind.TRANSFER -> "Transfer"; ActivityKind.SAVINGS -> "Savings"; ActivityKind.DEBT -> "Debt" },
                                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(amount, color = if (positive) MaterialTheme.colorScheme.primary else if (event.kind == ActivityKind.TRANSFER) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    datePickerTarget?.let { target ->
        key(target) {
            val initialDate = if (target == ActivityDatePickerTarget.START) customStartDate else customEndDate
            val todayUtcMillis = remember { todayUtcDatePickerMillis() }
            val todayYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialDate,
                selectableDates = remember(target, customStartDate, todayUtcMillis) {
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val notInFuture = utcTimeMillis <= todayUtcMillis
                            val notBeforeStart = target != ActivityDatePickerTarget.END ||
                                customStartDate == null || utcTimeMillis >= customStartDate!!
                            return notInFuture && notBeforeStart
                        }

                        override fun isSelectableYear(year: Int): Boolean = year <= todayYear
                    }
                }
            )
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null && selected <= todayUtcMillis) {
                        if (target == ActivityDatePickerTarget.START) {
                            customStartDate = selected
                            if (customEndDate != null && customEndDate!! < selected) customEndDate = null
                            dateRangeError = null
                            datePickerTarget = ActivityDatePickerTarget.END
                        } else {
                            if (customStartDate == null || selected >= customStartDate!!) {
                                customEndDate = selected
                                periodFilter = TransactionPeriodFilter.CUSTOM
                                dateRangeError = null
                                datePickerTarget = null
                            } else {
                                dateRangeError = "End date must be on or after the start date."
                            }
                        }
                    } else {
                        dateRangeError = if (selected == null) "Choose a date before continuing." else "Future dates are not allowed."
                    }
                }) { Text(if (target == ActivityDatePickerTarget.START) "Next" else "Apply range") }
            },
            dismissButton = { TextButton(onClick = { datePickerTarget = null }) { Text("Cancel") } }
            ) {
                Column {
                Text(
                    if (target == ActivityDatePickerTarget.START) "Choose start date" else "Choose end date",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    datePickerState.selectedDateMillis?.let {
                        "Selected: ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))}"
                    } ?: "Select a date, then tap ${if (target == ActivityDatePickerTarget.START) "Next" else "Apply range"}.",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DatePicker(state = datePickerState)
                dateRangeError?.let { error ->
                    Text(error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                }
            }
        }
    }

    selectedEvent?.let { event ->
        AlertDialog(onDismissRequest = { selectedEvent = null },
            title = { Text(when (event.kind) { ActivityKind.TRANSFER -> "Transfer details"; ActivityKind.SAVINGS -> "Savings details"; ActivityKind.DEBT -> "Debt details"; else -> "Activity details" }) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text("Amount: ${historyKes(event.amountMinor)}")
                Text("Details: ${event.subtitle}")
                Text("Date: ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(event.dateMillis))}")
            } },
            confirmButton = { TextButton(onClick = { selectedEvent = null }) { Text("Close") } })
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
                TextButton(onClick = { editingTransaction = transaction; selectedTransaction = null }) { TrackItIcon(R.drawable.ic_lucide_edit, null); Text("Edit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { deleteCandidate = transaction; selectedTransaction = null }) { TrackItIcon(R.drawable.ic_lucide_trash, null); Text("Delete") }
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
            dismissButton = { TextButton(onClick = { editingTransaction = null }) { TrackItIcon(R.drawable.ic_lucide_arrow_left, null); Text("Cancel") } }
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

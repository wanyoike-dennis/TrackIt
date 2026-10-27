package com.inventsolutions.trackit.ui.screens.hustles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.Hustle
import com.inventsolutions.trackit.data.local.HustleDebt
import com.inventsolutions.trackit.data.local.HustleTransaction
import com.inventsolutions.trackit.data.local.TrackItDatabase
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.NumberFormat
import java.util.Locale

private fun hustleKes(minor: Long): String =
    "KSh " + NumberFormat.getNumberInstance(Locale("en", "KE")).format(minor / 100.0)

@Composable
fun HustlesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { TrackItDatabase.getInstance(context).hustleDao() }
    val hustles by dao.observeAll().collectAsState(initial = emptyList())
    val transactions by dao.observeTransactions(null, 0L, Long.MAX_VALUE).collectAsState(initial = emptyList())
    val debts by dao.observeDebts(null).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf("Overview") }
    var selectedHustle by remember { mutableStateOf<Long?>(null) }
    var showAddHustle by remember { mutableStateOf(false) }
    var showAddRecord by remember { mutableStateOf(false) }
    var showAddDebt by remember { mutableStateOf(false) }
    var hustleName by remember { mutableStateOf("") }
    var hustleDescription by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(HustleDebt.CUSTOMER_OWES_ME) }
    var recordType by remember { mutableStateOf(HustleTransaction.INCOME) }
    var message by remember { mutableStateOf<String?>(null) }

    val scopedHustles = if (selectedHustle == null) hustles else hustles.filter { it.id == selectedHustle }
    val scopedTransactions = transactions.filter { selectedHustle == null || it.hustleId == selectedHustle }
    val income = scopedTransactions.filter { it.type == HustleTransaction.INCOME }.sumOf { it.amountMinor }
    val expenses = scopedTransactions.filter { it.type == HustleTransaction.EXPENSE }.sumOf { it.amountMinor }
    val scopedDebts = debts.filter { selectedHustle == null || it.hustleId == selectedHustle }
    val outstanding = scopedDebts.sumOf { debt ->
        (debt.originalAmountMinor - transactions.filter { false }.sumOf { it.amountMinor }).coerceAtLeast(0L)
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Side Hustles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Your independent earnings workspace", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onBack) { Text("Home") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Overview", "Hustles", "Records", "Debts", "Reports").forEach { item ->
                    OutlinedButton(onClick = { section = item }, modifier = Modifier.weight(1f)) {
                        Text(item.take(1), fontWeight = if (section == item) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            if (section == "Overview" || section == "Reports") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Earnings", hustleKes(income), Modifier.weight(1f))
                    MetricCard("Expenses", hustleKes(expenses), Modifier.weight(1f))
                }
                MetricCard("Net profit", hustleKes(income - expenses), Modifier.fillMaxWidth())
                Text("Outstanding debt records: ${scopedDebts.size}", style = MaterialTheme.typography.titleMedium)
                Text("Profit is calculated from hustle records only. Personal transactions and budgets are not included.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (section == "Hustles") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("My hustles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Button(onClick = { showAddHustle = true }) { Text("Add hustle") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hustles, key = { it.id }) { hustle ->
                        Card(Modifier.fillMaxWidth().clickable { selectedHustle = if (selectedHustle == hustle.id) null else hustle.id }) {
                            Column(Modifier.padding(14.dp)) {
                                Text(hustle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (hustle.description.isNotBlank()) Text(hustle.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (selectedHustle == hustle.id) "Selected · tap to show all hustles" else "Tap to filter by this hustle")
                                TextButton(onClick = { scope.launch { dao.update(hustle.copy(active = !hustle.active, updatedAtEpochMillis = System.currentTimeMillis())) } }) {
                                    Text(if (hustle.active) "Archive" else "Reactivate")
                                }
                            }
                        }
                    }
                }
            }
            if (section == "Records") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Earnings & expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Button(onClick = { showAddRecord = true }) { Text("Add record") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scopedTransactions, key = { it.id }) { record ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(record.description.ifBlank { record.category.ifBlank { "Hustle record" } }, fontWeight = FontWeight.SemiBold)
                                Text("${record.type} · ${hustles.firstOrNull { it.id == record.hustleId }?.name.orEmpty()}")
                                Text(hustleKes(if (record.type == HustleTransaction.EXPENSE) -record.amountMinor else record.amountMinor), color = if (record.type == HustleTransaction.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                TextButton(onClick = { scope.launch { dao.deleteTransaction(record.id) } }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
            if (section == "Debts") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Debt register", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Button(onClick = { showAddDebt = true }) { Text("Add debt") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scopedDebts, key = { it.id }) { debt ->
                        val settlements by dao.observeSettlements(debt.id).collectAsState(initial = emptyList())
                        val settled = settlements.sumOf { it.amountMinor }
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(debt.counterparty, fontWeight = FontWeight.SemiBold)
                                Text(if (debt.direction == HustleDebt.CUSTOMER_OWES_ME) "Owes me" else "I owe")
                                Text("Outstanding: ${hustleKes((debt.originalAmountMinor - settled).coerceAtLeast(0L))}")
                                var settlementAmount by remember(debt.id) { mutableStateOf("") }
                                OutlinedTextField(value = settlementAmount, onValueChange = { settlementAmount = it }, label = { Text("Settlement amount (KES)") }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    val minor = settlementAmount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
                                    if (minor == null || minor <= 0) message = "Enter a valid positive settlement amount."
                                    else scope.launch {
                                        runCatching { dao.settleDebt(debt.id, minor, "", System.currentTimeMillis()) }
                                            .onSuccess { settlementAmount = ""; message = "Settlement recorded." }
                                            .onFailure { message = it.message ?: "Could not record settlement." }
                                    }
                                }) { Text("Record settlement") }
                            }
                        }
                    }
                }
            }
            if (selectedHustle != null) TextButton(onClick = { selectedHustle = null }) { Text("Clear hustle filter") }
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showAddHustle) {
        AlertDialog(onDismissRequest = { showAddHustle = false }, title = { Text("Add a hustle") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(hustleName, { hustleName = it }, label = { Text("Hustle name") })
                OutlinedTextField(hustleDescription, { hustleDescription = it }, label = { Text("Description (optional)") })
            }
        }, confirmButton = { Button(onClick = {
            val clean = hustleName.trim()
            if (clean.isBlank()) message = "Enter a hustle name."
            else scope.launch {
                runCatching { dao.insert(Hustle(name = clean, normalizedName = clean.lowercase(Locale.ROOT), description = hustleDescription.trim())) }
                    .onSuccess { hustleName = ""; hustleDescription = ""; showAddHustle = false; message = null }
                    .onFailure { message = "A hustle with that name may already exist." }
            }
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showAddHustle = false }) { Text("Cancel") } })
    }
    if (showAddRecord) {
        AlertDialog(onDismissRequest = { showAddRecord = false }, title = { Text("Add hustle record") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { recordType = HustleTransaction.INCOME }) { Text("Earning") }
                    OutlinedButton(onClick = { recordType = HustleTransaction.EXPENSE }) { Text("Expense") }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") })
                OutlinedTextField(category, { category = it }, label = { Text("Category / service") })
                OutlinedTextField(note, { note = it }, label = { Text("Description (optional)") })
                Text("Hustle: ${hustles.firstOrNull { it.id == selectedHustle }?.name ?: "Choose one in My hustles"}")
            }
        }, confirmButton = { Button(onClick = {
            val target = selectedHustle
            val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
            if (target == null || hustles.none { it.id == target && it.active }) message = "Select an active hustle in My hustles first."
            else if (minor == null || minor <= 0) message = "Enter a valid positive amount."
            else scope.launch {
                dao.insertTransaction(HustleTransaction(hustleId = target, type = recordType, amountMinor = minor, category = category.trim(), description = note.trim()))
                amount = ""; category = ""; note = ""; showAddRecord = false; message = null
            }
        }) { Text("Save record") } }, dismissButton = { TextButton(onClick = { showAddRecord = false }) { Text("Cancel") } })
    }
    if (showAddDebt) {
        AlertDialog(onDismissRequest = { showAddDebt = false }, title = { Text("Add debt") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(counterparty, { counterparty = it }, label = { Text("Customer / supplier") })
                OutlinedTextField(amount, { amount = it }, label = { Text("Original amount (KES)") })
                OutlinedTextField(note, { note = it }, label = { Text("Description (optional)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { direction = HustleDebt.CUSTOMER_OWES_ME }) { Text("Owes me") }
                    OutlinedButton(onClick = { direction = HustleDebt.I_OWE }) { Text("I owe") }
                }
                Text("Select a hustle first in My hustles.")
            }
        }, confirmButton = { Button(onClick = {
            val target = selectedHustle
            val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
            if (target == null || hustles.none { it.id == target && it.active }) message = "Select an active hustle in My hustles first."
            else if (counterparty.isBlank() || minor == null || minor <= 0) message = "Enter a counterparty and valid positive amount."
            else scope.launch {
                dao.insertDebt(HustleDebt(hustleId = target, direction = direction, counterparty = counterparty.trim(), description = note.trim(), originalAmountMinor = minor))
                counterparty = ""; amount = ""; note = ""; showAddDebt = false; message = null
            }
        }) { Text("Save debt") } }, dismissButton = { TextButton(onClick = { showAddDebt = false }) { Text("Cancel") } })
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

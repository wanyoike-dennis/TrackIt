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
    val debtBalances by dao.observeDebtBalances().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var section by remember { mutableStateOf("Overview") }
    var selectedHustleId by remember { mutableStateOf<Long?>(null) }
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

    val selectedHustle = hustles.firstOrNull { it.id == selectedHustleId && it.active }
    val scopedTransactions = transactions.filter { selectedHustleId == null || it.hustleId == selectedHustleId }
    val scopedDebts = debts.filter { selectedHustleId == null || it.hustleId == selectedHustleId }
    val income = scopedTransactions.filter { it.type == HustleTransaction.INCOME }.sumOf { it.amountMinor }
    val expenses = scopedTransactions.filter { it.type == HustleTransaction.EXPENSE }.sumOf { it.amountMinor }
    val outstanding = debtBalances.filter { balance -> scopedDebts.any { it.id == balance.debtId } }.sumOf { it.outstandingAmountMinor }
    val recentTransactions = scopedTransactions.sortedByDescending { it.dateEpochMillis }.take(5)

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Side Hustles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Track what you earn from your work", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onBack) { Text("Back") }
            }

            // Make the current scope explicit and accessible before any money is entered.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("VIEWING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {}
                ) {
                    // A straightforward selector button opens the complete hustle list below.
                    OutlinedButton(onClick = { section = "My Hustles" }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedHustle?.name ?: "All hustles", modifier = Modifier.weight(1f))
                        Text("Change")
                    }
                }
                if (hustles.isEmpty()) {
                    Text("Create your first hustle to start recording earnings and expenses.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showAddHustle = true }, modifier = Modifier.fillMaxWidth()) { Text("Create my first hustle") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { recordType = HustleTransaction.INCOME; showAddRecord = true },
                    enabled = hustles.any { it.active },
                    modifier = Modifier.weight(1f)
                ) { Text("＋ Add earning") }
                OutlinedButton(
                    onClick = { recordType = HustleTransaction.EXPENSE; showAddRecord = true },
                    enabled = hustles.any { it.active },
                    modifier = Modifier.weight(1f)
                ) { Text("− Add expense") }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Earnings", hustleKes(income), Modifier.weight(1f))
                MetricCard("Expenses", hustleKes(expenses), Modifier.weight(1f))
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Net profit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(hustleKes(income - expenses), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Earnings minus recorded expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Card(Modifier.fillMaxWidth().clickable { section = "Debts" }) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Outstanding debts", fontWeight = FontWeight.SemiBold)
                        Text(hustleKes(outstanding), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${scopedDebts.size} debt records · View and record repayments", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { section = "Debts" }) { Text("Manage") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { section = "Records" }) { Text("View all") }
            }
            if (recentTransactions.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No records yet", fontWeight = FontWeight.SemiBold)
                        Text("Your hustle earnings and expenses will appear here after you add them.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                    items(recentTransactions, key = { it.id }) { record ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(record.description.ifBlank { record.category.ifBlank { if (record.type == HustleTransaction.INCOME) "Earning" else "Expense" } }, fontWeight = FontWeight.SemiBold)
                                    Text(hustles.firstOrNull { it.id == record.hustleId }?.name.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    (if (record.type == HustleTransaction.INCOME) "+" else "−") + hustleKes(record.amountMinor),
                                    color = if (record.type == HustleTransaction.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { section = "My Hustles" }, modifier = Modifier.weight(1f)) { Text("My hustles") }
                OutlinedButton(onClick = { section = "Reports" }, modifier = Modifier.weight(1f)) { Text("Reports") }
            }
            if (message != null) Text(message!!, color = MaterialTheme.colorScheme.error)
        }
    }

    if (section != "Overview") {
        AlertDialog(
            onDismissRequest = { section = "Overview" },
            title = { Text(when (section) { "My Hustles" -> "My hustles"; "Records" -> "All records"; "Debts" -> "Manage debts"; else -> "Hustle reports" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    when (section) {
                        "My Hustles" -> {
                            Text("Choose a hustle to view its finances, or create a new one.")
                            hustles.forEach { hustle ->
                                Card(Modifier.fillMaxWidth().clickable {
                                    selectedHustleId = hustle.id
                                    section = "Overview"
                                }) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(hustle.name, fontWeight = FontWeight.SemiBold)
                                            Text(if (hustle.active) "Active" else "Archived", style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(onClick = {
                                            scope.launch { dao.update(hustle.copy(active = !hustle.active, updatedAtEpochMillis = System.currentTimeMillis())) }
                                        }) { Text(if (hustle.active) "Archive" else "Restore") }
                                    }
                                }
                            }
                            Button(onClick = { showAddHustle = true; section = "Overview" }, modifier = Modifier.fillMaxWidth()) { Text("＋ Create a hustle") }
                            if (selectedHustleId != null) TextButton(onClick = { selectedHustleId = null; section = "Overview" }) { Text("Show all hustles") }
                        }
                        "Records" -> {
                            if (scopedTransactions.isEmpty()) Text("No earnings or expenses recorded yet.")
                            scopedTransactions.sortedByDescending { it.dateEpochMillis }.take(30).forEach { record ->
                                Text("${record.description.ifBlank { record.category.ifBlank { record.type.lowercase().replaceFirstChar { it.uppercase() } } }} · ${hustleKes(record.amountMinor)}")
                                TextButton(onClick = { scope.launch { dao.deleteTransaction(record.id) } }) { Text("Delete record") }
                            }
                        }
                        "Debts" -> {
                            Button(onClick = { showAddDebt = true; section = "Overview" }, modifier = Modifier.fillMaxWidth(), enabled = hustles.any { it.active }) { Text("＋ Add debt") }
                            if (scopedDebts.isEmpty()) Text("No debts recorded for this selection.")
                            scopedDebts.forEach { debt ->
                                val settlements by dao.observeSettlements(debt.id).collectAsState(initial = emptyList())
                                val settled = settlements.sumOf { it.amountMinor }
                                Text("${debt.counterparty} · ${if (debt.direction == HustleDebt.CUSTOMER_OWES_ME) "Owes you" else "You owe"}")
                                Text("Outstanding: ${hustleKes((debt.originalAmountMinor - settled).coerceAtLeast(0L))}")
                                var settlementAmount by remember(debt.id) { mutableStateOf("") }
                                OutlinedTextField(settlementAmount, { settlementAmount = it }, label = { Text("Repayment amount (KES)") }, modifier = Modifier.fillMaxWidth())
                                TextButton(onClick = {
                                    val minor = settlementAmount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
                                    if (minor == null || minor <= 0) message = "Enter a valid repayment amount."
                                    else scope.launch {
                                        runCatching { dao.settleDebt(debt.id, minor, "", System.currentTimeMillis()) }
                                            .onSuccess { settlementAmount = ""; message = "Repayment saved." }
                                            .onFailure { message = it.message ?: "Could not save repayment." }
                                    }
                                }) { Text("Save repayment") }
                            }
                        }
                        "Reports" -> {
                            Text("All-time summary for the selected hustle scope")
                            Text("Total earnings: ${hustleKes(income)}")
                            Text("Total expenses: ${hustleKes(expenses)}")
                            Text("Net profit: ${hustleKes(income - expenses)}")
                            Text("Outstanding debts: ${hustleKes(outstanding)}")
                            Text("This initial report uses all recorded dates. Date filters will be added separately.")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { section = "Overview" }) { Text("Done") } }
        )
    }

    if (showAddHustle) {
        AlertDialog(onDismissRequest = { showAddHustle = false }, title = { Text("Create a hustle") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(hustleName, { hustleName = it }, label = { Text("Hustle name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(hustleDescription, { hustleDescription = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = { Button(onClick = {
            val clean = hustleName.trim()
            if (clean.isBlank()) message = "Enter a hustle name."
            else scope.launch {
                runCatching { dao.insert(Hustle(name = clean, normalizedName = clean.lowercase(Locale.ROOT), description = hustleDescription.trim())) }
                    .onSuccess {
                        hustleName = ""; hustleDescription = ""; showAddHustle = false; message = null
                        selectedHustleId = it
                    }
                    .onFailure { message = "A hustle with that name may already exist." }
            }
        }) { Text("Create hustle") } }, dismissButton = { TextButton(onClick = { showAddHustle = false }) { Text("Cancel") } })
    }

    if (showAddRecord) {
        val activeHustles = hustles.filter { it.active }
        AlertDialog(onDismissRequest = { showAddRecord = false }, title = { Text(if (recordType == HustleTransaction.INCOME) "Add earning" else "Add expense") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { recordType = HustleTransaction.INCOME }) { Text("Earning") }
                    OutlinedButton(onClick = { recordType = HustleTransaction.EXPENSE }) { Text("Expense") }
                }
                Text("Which hustle?", fontWeight = FontWeight.SemiBold)
                activeHustles.forEach { hustle ->
                    OutlinedButton(onClick = { selectedHustleId = hustle.id }, modifier = Modifier.fillMaxWidth()) {
                        Text((if (selectedHustleId == hustle.id) "✓ " else "") + hustle.name)
                    }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category or service (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        }, confirmButton = { Button(onClick = {
            val target = selectedHustleId
            val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
            if (target == null || activeHustles.none { it.id == target }) message = "Choose an active hustle."
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
                Text("Which hustle is this debt for?", fontWeight = FontWeight.SemiBold)
                hustles.filter { it.active }.forEach { hustle ->
                    OutlinedButton(onClick = { selectedHustleId = hustle.id }, modifier = Modifier.fillMaxWidth()) {
                        Text((if (selectedHustleId == hustle.id) "✓ " else "") + hustle.name)
                    }
                }
                OutlinedTextField(counterparty, { counterparty = it }, label = { Text("Customer or supplier") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { direction = HustleDebt.CUSTOMER_OWES_ME }) { Text("Owes me") }
                    OutlinedButton(onClick = { direction = HustleDebt.I_OWE }) { Text("I owe") }
                }
            }
        }, confirmButton = { Button(onClick = {
            val target = selectedHustleId
            val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
            if (target == null || hustles.none { it.id == target && it.active }) message = "Choose an active hustle."
            else if (counterparty.isBlank() || minor == null || minor <= 0) message = "Enter a name and a valid amount."
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

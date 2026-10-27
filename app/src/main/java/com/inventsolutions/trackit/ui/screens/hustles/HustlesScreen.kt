package com.inventsolutions.trackit.ui.screens.hustles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(com.inventsolutions.trackit.ui.theme.TrackItEmeraldTint),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("↗", color = com.inventsolutions.trackit.ui.theme.TrackItEmeraldDark, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Side Hustles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Your work. Your progress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = onBack) { Text("Close") }
            }

            // A dark, high-contrast profit hero gives the module a clear visual anchor.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.inventsolutions.trackit.ui.theme.TrackItNavy)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("TOTAL NET PROFIT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C2D1), fontWeight = FontWeight.SemiBold)
                            Text("All-time overview", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C2D1))
                        }
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF243B35)),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("↗", color = Color(0xFF6EE7B7), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        hustleKes(income - expenses),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF172B2A)).padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Earnings", color = Color(0xFFB8C2D1), style = MaterialTheme.typography.labelSmall)
                            Text(hustleKes(income), color = Color(0xFF6EE7B7), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Expenses", color = Color(0xFFB8C2D1), style = MaterialTheme.typography.labelSmall)
                            Text(hustleKes(expenses), color = Color(0xFFFCA5A5), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Your hustles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(selectedHustle?.name ?: "Combined performance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { section = "My Hustles" }) { Text("Manage") }
                }
                Card(
                    Modifier.fillMaxWidth().clickable { section = "My Hustles" },
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("▦", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(selectedHustle?.name ?: "All hustles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${hustles.count { it.active }} active hustle(s) · ${hustles.size} total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                    }
                }
                if (hustles.isEmpty()) {
                    Text("Start by adding the work you do, then record what you earn and spend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { showAddHustle = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("＋  Add your first hustle") }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(
                        title = "Add earning",
                        subtitle = "Money received",
                        symbol = "↗",
                        accent = Color(0xFF16845B),
                        tint = Color(0xFFE1F7EC),
                        modifier = Modifier.weight(1f),
                        enabled = hustles.any { it.active }
                    ) { recordType = HustleTransaction.INCOME; showAddRecord = true }
                    ActionTile(
                        title = "Add expense",
                        subtitle = "Money spent",
                        symbol = "↘",
                        accent = Color(0xFFC44949),
                        tint = Color(0xFFFDECEC),
                        modifier = Modifier.weight(1f),
                        enabled = hustles.any { it.active }
                    ) { recordType = HustleTransaction.EXPENSE; showAddRecord = true }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Financial snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("All time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SnapshotTile("Earnings", hustleKes(income), "↗", Color(0xFF16845B), Color(0xFFE1F7EC), Modifier.weight(1f))
                    SnapshotTile("Expenses", hustleKes(expenses), "↘", Color(0xFFC44949), Color(0xFFFDECEC), Modifier.weight(1f))
                }
                Card(
                    Modifier.fillMaxWidth().clickable { section = "Debts" },
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF3D6)), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("◇", color = Color(0xFF9A6700), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Outstanding debts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("${scopedDebts.size} records · Track repayments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(hustleKes(outstanding), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("View details  ›", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("The latest hustle transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { section = "Records" }) { Text("See all") }
                }
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (recentTransactions.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.foundation.layout.Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                Text("↗", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Your story starts here", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("Add an earning or expense to see your activity here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column {
                            recentTransactions.forEachIndexed { index, record ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { section = "Records" }.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val isIncome = record.type == HustleTransaction.INCOME
                                    androidx.compose.foundation.layout.Box(
                                        Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (isIncome) Color(0xFFE1F7EC) else Color(0xFFFDECEC)),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Text(if (isIncome) "↗" else "↘", color = if (isIncome) Color(0xFF16845B) else Color(0xFFC44949), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(record.description.ifBlank { record.category.ifBlank { if (isIncome) "Earning" else "Expense" } }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(hustles.firstOrNull { it.id == record.hustleId }?.name.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Text(
                                        (if (isIncome) "+" else "−") + hustleKes(record.amountMinor),
                                        color = if (isIncome) Color(0xFF16845B) else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                if (index < recentTransactions.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { section = "My Hustles" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("Manage hustles") }
                OutlinedButton(onClick = { section = "Reports" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("View reports") }
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
private fun ActionTile(
    title: String,
    subtitle: String,
    symbol: String,
    accent: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tint),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(symbol, color = accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SnapshotTile(
    title: String,
    value: String,
    symbol: String,
    accent: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tint),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(symbol, color = accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
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

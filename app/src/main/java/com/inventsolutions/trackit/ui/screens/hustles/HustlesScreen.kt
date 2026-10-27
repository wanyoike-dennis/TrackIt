package com.inventsolutions.trackit.ui.screens.hustles

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventsolutions.trackit.data.local.Hustle
import com.inventsolutions.trackit.data.local.HustleDebt
import com.inventsolutions.trackit.data.local.HustleTransaction
import com.inventsolutions.trackit.data.local.TrackItDatabase
import com.inventsolutions.trackit.ui.theme.TrackItEmerald
import com.inventsolutions.trackit.ui.theme.TrackItEmeraldDark
import com.inventsolutions.trackit.ui.theme.TrackItEmeraldTint
import com.inventsolutions.trackit.ui.theme.TrackItExpense
import com.inventsolutions.trackit.ui.theme.TrackItNavy
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val StudioMuted = Color(0xFF667085)
private val StudioLine = Color(0xFFE4E7EC)
private val StudioGreen = Color(0xFF16A36A)
private val StudioGreenTint = Color(0xFFE7F7EF)
private val StudioRedTint = Color(0xFFFDECEC)
private val StudioAmberTint = Color(0xFFFFF3D6)

private fun hustleKes(minor: Long): String =
    "KSh " + NumberFormat.getNumberInstance(Locale("en", "KE")).format(minor / 100.0)

private fun hustleDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

private val studioSections = listOf("Overview", "Hustles", "Activity", "Debts", "Reports")

@Composable
fun HustlesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { TrackItDatabase.getInstance(context).hustleDao() }
    val hustles by dao.observeAll().collectAsState(initial = emptyList())
    val transactions by dao.observeTransactions(null, 0L, Long.MAX_VALUE).collectAsState(initial = emptyList())
    val debts by dao.observeDebts(null).collectAsState(initial = emptyList())
    val balances by dao.observeDebtBalances().collectAsState(initial = emptyList())
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
    var debtToSettle by remember { mutableStateOf<HustleDebt?>(null) }
    var settlementAmount by remember { mutableStateOf("") }

    val activeHustles = hustles.filter { it.active }
    val selectedHustle = activeHustles.firstOrNull { it.id == selectedHustleId }
    val scopedTransactions = transactions.filter { selectedHustleId == null || it.hustleId == selectedHustleId }
    val scopedDebts = debts.filter { selectedHustleId == null || it.hustleId == selectedHustleId }
    val income = scopedTransactions.filter { it.type == HustleTransaction.INCOME }.sumOf { it.amountMinor }
    val expenses = scopedTransactions.filter { it.type == HustleTransaction.EXPENSE }.sumOf { it.amountMinor }
    val profit = income - expenses
    val scopedBalances = balances.filter { item -> scopedDebts.any { it.id == item.debtId } }
    val owedToMe = scopedDebts.filter { it.direction == HustleDebt.CUSTOMER_OWES_ME }.sumOf { debt ->
        scopedBalances.firstOrNull { it.debtId == debt.id }?.outstandingAmountMinor ?: debt.originalAmountMinor
    }
    val iOwe = scopedDebts.filter { it.direction == HustleDebt.I_OWE }.sumOf { debt ->
        scopedBalances.firstOrNull { it.debtId == debt.id }?.outstandingAmountMinor ?: debt.originalAmountMinor
    }
    val recent = scopedTransactions.sortedByDescending { it.dateEpochMillis }.take(5)

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { inset ->
        Column(Modifier.fillMaxSize().padding(inset)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(TrackItNavy), contentAlignment = Alignment.Center) {
                    StudioIcon("trend", Color.White, Modifier.size(23.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("TRACKIT  /  STUDIO", style = MaterialTheme.typography.labelSmall, color = StudioMuted, letterSpacing = 1.2.sp)
                    Text("Side hustles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onBack) { Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                studioSections.forEach { tab ->
                    val selected = section == tab
                    Surface(Modifier.clip(CircleShape).clickable { section = tab }, shape = CircleShape,
                        color = if (selected) TrackItNavy else Color.Transparent,
                        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, StudioLine)) {
                        Text(tab, Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
            HorizontalDivider(color = StudioLine)

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                when (section) {
                    "Overview" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(selectedHustle?.name ?: "Your business, at a glance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(if (selectedHustle == null) "Every hustle. One clear picture." else selectedHustle.description.ifBlank { "Your hustle performance" },
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip("All hustles", selectedHustleId == null) { selectedHustleId = null }
                                activeHustles.forEach { hustle -> FilterChip(hustle.name, selectedHustleId == hustle.id) { selectedHustleId = hustle.id } }
                            }
                        }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(TrackItNavy).padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("NET PROFIT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C2D1), letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text("All-time performance", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C2D1))
                                }
                                Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF203A32)), contentAlignment = Alignment.Center) {
                                    StudioIcon("trend", Color(0xFF6EE7B7), Modifier.size(23.dp))
                                }
                            }
                            Text(hustleKes(profit), style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Earnings", color = Color(0xFFB8C2D1), style = MaterialTheme.typography.bodySmall)
                                    Text(hustleKes(income), color = Color(0xFF6EE7B7), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                                val flow = max(1L, income + expenses)
                                Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0xFF344054))) {
                                    Box(Modifier.fillMaxWidth((income.toFloat() / flow.toFloat()).coerceIn(0f, 1f)).height(5.dp).clip(CircleShape).background(Color(0xFF34D399))
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Expenses", color = Color(0xFFB8C2D1), style = MaterialTheme.typography.bodySmall)
                                    Text(hustleKes(expenses), color = Color(0xFFFDA29B), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF344054))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("RECORDS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A2B3), letterSpacing = 1.sp)
                                    Text(scopedTransactions.size.toString(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("TO COLLECT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF98A2B3), letterSpacing = 1.sp)
                                    Text(hustleKes(owedToMe), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Make a move", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Keep your records up to date.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                QuickAction("Add earning", "Money in", "income", Modifier.weight(1f), activeHustles.isNotEmpty()) { recordType = HustleTransaction.INCOME; showAddRecord = true }
                                QuickAction("Add expense", "Money out", "expense", Modifier.weight(1f), activeHustles.isNotEmpty()) { recordType = HustleTransaction.EXPENSE; showAddRecord = true }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryAction("New hustle", "briefcase", Modifier.weight(1f)) { showAddHustle = true }
                                SecondaryAction("Add debt", "wallet", Modifier.weight(1f), activeHustles.isNotEmpty()) { showAddDebt = true }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeading("Your hustles", "See how each one is doing") { section = "Hustles" }
                            if (activeHustles.isEmpty()) EmptyState("briefcase", "Start with your first hustle", "Add the work you do, then track earnings and expenses.") { showAddHustle = true }
                            else activeHustles.take(3).forEachIndexed { index, hustle ->
                                val records = transactions.filter { it.hustleId == hustle.id }
                                val inTotal = records.filter { it.type == HustleTransaction.INCOME }.sumOf { it.amountMinor }
                                val outTotal = records.filter { it.type == HustleTransaction.EXPENSE }.sumOf { it.amountMinor }
                                HustleRow(hustle, if (hustle.description.isNotBlank()) hustle.description else records.size.toString() + " records",
                                    hustleKes(inTotal - outTotal), index, { selectedHustleId = hustle.id; section = "Overview" })
                            }
                            if (activeHustles.size > 3) TextButton(onClick = { section = "Hustles" }) { Text("View all " + activeHustles.size + " hustles") }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeading("Recent activity", "Latest income and expenses") { section = "Activity" }
                            if (recent.isEmpty()) EmptyState("receipt", "Nothing recorded yet", "Your latest hustle activity will appear here.") {
                                if (activeHustles.isNotEmpty()) { recordType = HustleTransaction.INCOME; showAddRecord = true } else showAddHustle = true
                            } else recent.forEachIndexed { index, record ->
                                ActivityRow(record, hustles.firstOrNull { it.id == record.hustleId }?.name.orEmpty())
                                if (index < recent.lastIndex) HorizontalDivider(color = StudioLine, modifier = Modifier.padding(start = 48.dp))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionHeading("Debt position", "Outstanding balances") { section = "Debts" }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MiniBalance("To collect", hustleKes(owedToMe), "wallet", Modifier.weight(1f), StudioGreen)
                                MiniBalance("To pay", hustleKes(iOwe), "receipt", Modifier.weight(1f), TrackItExpense)
                            }
                        }
                    }
                    "Hustles" -> {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Your hustles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(activeHustles.size.toString() + " active · " + (hustles.size - activeHustles.size) + " archived",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { showAddHustle = true }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = TrackItNavy)) {
                                StudioIcon("plus", Color.White, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add")
                            }
                        }
                        if (hustles.isEmpty()) EmptyState("briefcase", "Your next income stream starts here", "Create a hustle for your side work, services, or small business.") { showAddHustle = true }
                        else hustles.forEachIndexed { index, hustle ->
                            val records = transactions.filter { it.hustleId == hustle.id }
                            val inTotal = records.filter { it.type == HustleTransaction.INCOME }.sumOf { it.amountMinor }
                            val outTotal = records.filter { it.type == HustleTransaction.EXPENSE }.sumOf { it.amountMinor }
                            HustleRow(hustle, if (hustle.active) records.size.toString() + " transactions" else "Archived",
                                hustleKes(inTotal - outTotal), index, { selectedHustleId = hustle.id; section = "Overview" }) {
                                TextButton(onClick = { scope.launch { dao.update(hustle.copy(active = !hustle.active, updatedAtEpochMillis = System.currentTimeMillis())) } }) {
                                    Text(if (hustle.active) "Archive" else "Restore")
                                }
                            }
                        }
                    }
                    "Activity" -> {
                        Text("Activity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("A clean ledger of every earning and expense.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip("All hustles", selectedHustleId == null) { selectedHustleId = null }
                            activeHustles.forEach { hustle -> FilterChip(hustle.name, selectedHustleId == hustle.id) { selectedHustleId = hustle.id } }
                        }
                        if (scopedTransactions.isEmpty()) EmptyState("receipt", "No activity to show", "Record an earning or expense to start your ledger.") {
                            if (activeHustles.isNotEmpty()) { recordType = HustleTransaction.INCOME; showAddRecord = true } else showAddHustle = true
                        } else {
                            Text(scopedTransactions.size.toString() + " records · All time", style = MaterialTheme.typography.labelMedium, color = StudioMuted)
                            scopedTransactions.sortedByDescending { it.dateEpochMillis }.forEachIndexed { index, record ->
                                ActivityRow(record, hustles.firstOrNull { it.id == record.hustleId }?.name.orEmpty(), true)
                                if (index < scopedTransactions.lastIndex) HorizontalDivider(color = StudioLine, modifier = Modifier.padding(start = 48.dp))
                            }
                        }
                    }
                    "Debts" -> {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Debt tracker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("Know what is owed to you and what you owe.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { showAddDebt = true }, enabled = activeHustles.isNotEmpty(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = TrackItNavy)) {
                                StudioIcon("plus", Color.White, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Add")
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniBalance("Owed to you", hustleKes(owedToMe), "wallet", Modifier.weight(1f), StudioGreen)
                            MiniBalance("You owe", hustleKes(iOwe), "receipt", Modifier.weight(1f), TrackItExpense)
                        }
                        if (scopedDebts.isEmpty()) EmptyState("wallet", "No debts recorded", "Track customer balances, supplier credit, or money you owe.") { if (activeHustles.isNotEmpty()) showAddDebt = true }
                        else scopedDebts.forEach { debt ->
                            DebtRow(debt, hustles.firstOrNull { it.id == debt.hustleId }?.name.orEmpty(),
                                scopedBalances.firstOrNull { it.debtId == debt.id }?.settledAmountMinor ?: 0L) { debtToSettle = debt }
                        }
                    }
                    "Reports" -> {
                        Text("Hustle reports", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Your all-time financial picture.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(TrackItNavy).padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("NET PROFIT", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8C2D1), letterSpacing = 1.4.sp)
                            Text(hustleKes(profit), style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(scopedTransactions.size.toString() + " transactions recorded", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C2D1))
                        }
                        ReportLine("Total earnings", hustleKes(income), StudioGreen); HorizontalDivider(color = StudioLine)
                        ReportLine("Total expenses", hustleKes(expenses), TrackItExpense); HorizontalDivider(color = StudioLine)
                        ReportLine("Net profit", hustleKes(profit), if (profit >= 0) StudioGreen else TrackItExpense); HorizontalDivider(color = StudioLine)
                        ReportLine("Owed to you", hustleKes(owedToMe), StudioMuted); HorizontalDivider(color = StudioLine)
                        ReportLine("You owe", hustleKes(iOwe), StudioMuted)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Earnings vs expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val flow = max(1L, income + expenses)
                            Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(StudioRedTint)) {
                                Box(Modifier.fillMaxWidth((income.toFloat() / flow.toFloat()).coerceIn(0f, 1f)).height(10.dp).background(StudioGreen))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Earnings " + if (income + expenses == 0L) "0%" else (income * 100 / (income + expenses)).toString() + "%",
                                    style = MaterialTheme.typography.bodySmall, color = StudioMuted)
                                Text("Expenses " + if (income + expenses == 0L) "0%" else (expenses * 100 / (income + expenses)).toString() + "%",
                                    style = MaterialTheme.typography.bodySmall, color = StudioMuted)
                            }
                        }
                        Text("Reports include all recorded dates. Date-range reporting is not included in this redesign.", style = MaterialTheme.typography.bodySmall, color = StudioMuted)
                    }
                }
                if (message != null) Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(message!!, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (debtToSettle != null) {
        val debt = debtToSettle!!
        val settled = scopedBalances.firstOrNull { it.debtId == debt.id }?.settledAmountMinor ?: 0L
        val remaining = (debt.originalAmountMinor - settled).coerceAtLeast(0L)
        AlertDialog(onDismissRequest = { debtToSettle = null; settlementAmount = "" }, title = { Text("Record repayment") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(debt.counterparty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Outstanding: " + hustleKes(remaining), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(settlementAmount, { settlementAmount = it }, label = { Text("Repayment amount (KES)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            } },
            confirmButton = { Button(onClick = {
                val minor = settlementAmount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
                if (minor == null || minor <= 0L) message = "Enter a valid repayment amount."
                else scope.launch {
                    runCatching { dao.settleDebt(debt.id, minor, "", System.currentTimeMillis()) }
                        .onSuccess { debtToSettle = null; settlementAmount = ""; message = "Repayment saved." }
                        .onFailure { message = it.message ?: "Could not save repayment." }
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = TrackItNavy)) { Text("Save repayment") } },
            dismissButton = { TextButton(onClick = { debtToSettle = null; settlementAmount = "" }) { Text("Cancel") } })
    }

    if (showAddHustle) AlertDialog(onDismissRequest = { showAddHustle = false }, title = { Text("Create a hustle") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Give this income stream a name. You can add more details later.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(hustleName, { hustleName = it }, label = { Text("Hustle name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(hustleDescription, { hustleDescription = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
        } },
        confirmButton = { Button(onClick = {
            val clean = hustleName.trim()
            if (clean.isBlank()) message = "Enter a hustle name."
            else scope.launch {
                runCatching { dao.insert(Hustle(name = clean, normalizedName = clean.lowercase(Locale.ROOT), description = hustleDescription.trim())) }
                    .onSuccess { hustleName = ""; hustleDescription = ""; showAddHustle = false; message = null; selectedHustleId = it; section = "Overview" }
                    .onFailure { message = "A hustle with that name may already exist." }
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = TrackItNavy)) { Text("Create hustle") } },
        dismissButton = { TextButton(onClick = { showAddHustle = false }) { Text("Cancel") } })

    if (showAddRecord) {
        val active = activeHustles
        var targetHustleId by remember { mutableStateOf(selectedHustleId ?: active.firstOrNull()?.id) }
        AlertDialog(onDismissRequest = { showAddRecord = false }, title = { Text(if (recordType == HustleTransaction.INCOME) "Add earning" else "Add expense") },
            text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp)) {
                    listOf(HustleTransaction.INCOME, HustleTransaction.EXPENSE).forEach { type ->
                        val selected = recordType == type
                        Box(Modifier.weight(1f).clip(CircleShape).background(if (selected) TrackItNavy else Color.Transparent)
                            .clickable { recordType = type }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (type == HustleTransaction.INCOME) "Earning" else "Expense", color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text("Which hustle?", style = MaterialTheme.typography.labelLarge)
                active.forEach { hustle ->
                    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { targetHustleId = hustle.id },
                        shape = RoundedCornerShape(12.dp), color = if (targetHustleId == hustle.id) TrackItEmeraldTint else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (targetHustleId == hustle.id) TrackItEmerald else StudioLine)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(hustle.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            if (targetHustleId == hustle.id) StudioIcon("check", TrackItEmeraldDark, Modifier.size(18.dp))
                        }
                    }
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Category or service (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
            } },
            confirmButton = { Button(onClick = {
                val target = targetHustleId
                val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
                if (target == null || active.none { it.id == target }) message = "Choose an active hustle."
                else if (minor == null || minor <= 0L) message = "Enter a valid positive amount."
                else scope.launch {
                    dao.insertTransaction(HustleTransaction(hustleId = target, type = recordType, amountMinor = minor, category = category.trim(), description = note.trim()))
                    amount = ""; category = ""; note = ""; showAddRecord = false; message = "Record saved."
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = TrackItNavy)) { Text("Save record") } },
            dismissButton = { TextButton(onClick = { showAddRecord = false }) { Text("Cancel") } })
    }

    if (showAddDebt) {
        val active = activeHustles
        var targetHustleId by remember { mutableStateOf(selectedHustleId ?: active.firstOrNull()?.id) }
        AlertDialog(onDismissRequest = { showAddDebt = false }, title = { Text("Add debt") },
            text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Link this balance to a hustle to track it with your business activity.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                active.forEach { hustle ->
                    Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { targetHustleId = hustle.id },
                        shape = RoundedCornerShape(12.dp), color = if (targetHustleId == hustle.id) TrackItEmeraldTint else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (targetHustleId == hustle.id) TrackItEmerald else StudioLine)) {
                        Text(hustle.name, Modifier.padding(12.dp), fontWeight = FontWeight.Medium)
                    }
                }
                OutlinedTextField(counterparty, { counterparty = it }, label = { Text("Customer or supplier") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KES)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
                Text("Debt direction", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(HustleDebt.CUSTOMER_OWES_ME, HustleDebt.I_OWE).forEach { value ->
                        val selected = direction == value
                        Surface(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { direction = value },
                            shape = RoundedCornerShape(12.dp), color = if (selected) TrackItNavy else MaterialTheme.colorScheme.surfaceVariant) {
                            Text(if (value == HustleDebt.CUSTOMER_OWES_ME) "Owes me" else "I owe", Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } },
            confirmButton = { Button(onClick = {
                val target = targetHustleId
                val minor = amount.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
                if (target == null || active.none { it.id == target }) message = "Choose an active hustle."
                else if (counterparty.isBlank() || minor == null || minor <= 0L) message = "Enter a name and a valid amount."
                else scope.launch {
                    dao.insertDebt(HustleDebt(hustleId = target, direction = direction, counterparty = counterparty.trim(), description = note.trim(), originalAmountMinor = minor))
                    counterparty = ""; amount = ""; note = ""; showAddDebt = false; message = "Debt saved."
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = TrackItNavy)) { Text("Save debt") } },
            dismissButton = { TextButton(onClick = { showAddDebt = false }) { Text("Cancel") } })
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(Modifier.clip(CircleShape).clickable(onClick = onClick), shape = CircleShape,
        color = if (selected) TrackItEmeraldTint else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) TrackItEmerald.copy(alpha = 0.45f) else StudioLine)) {
        Text(label, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = if (selected) TrackItEmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) { Text("See all") }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick) { Text("See all") }
    }
}

@Composable
private fun HustleRow(hustle: Hustle, subtitle: String, value: String, index: Int, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        val tile = when (index % 3) { 0 -> TrackItNavy; 1 -> TrackItEmeraldTint; else -> StudioAmberTint }
        val iconColor = when (index % 3) { 0 -> Color.White; 1 -> TrackItEmeraldDark; else -> Color(0xFF9A6700) }
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tile), contentAlignment = Alignment.Center) {
            StudioIcon("briefcase", iconColor, Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(hustle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (value.contains("-")) TrackItExpense else MaterialTheme.colorScheme.onSurface, maxLines = 1)
            if (trailing != null) trailing() else Text(if (hustle.active) "ACTIVE" else "ARCHIVED", style = MaterialTheme.typography.labelSmall,
                color = if (hustle.active) TrackItEmeraldDark else StudioMuted, letterSpacing = 0.5.sp)
        }
    }
    HorizontalDivider(color = StudioLine)
}

@Composable
private fun ActivityRow(record: HustleTransaction, hustleName: String, showDate: Boolean = false) {
    val isIncome = record.type == HustleTransaction.INCOME
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(if (isIncome) StudioGreenTint else StudioRedTint), contentAlignment = Alignment.Center) {
            StudioIcon(if (isIncome) "income" else "expense", if (isIncome) StudioGreen else TrackItExpense, Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(record.description.ifBlank { record.category.ifBlank { if (isIncome) "Earning" else "Expense" } }, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val detail = hustleName.ifBlank { "Hustle" } + if (showDate) " · " + hustleDate(record.dateEpochMillis) else if (record.category.isNotBlank()) " · " + record.category else ""
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text((if (isIncome) "+" else "−") + hustleKes(record.amountMinor), color = if (isIncome) StudioGreen else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun MiniBalance(label: String, value: String, icon: String, modifier: Modifier, accent: Color) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StudioIcon(icon, accent, Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun DebtRow(debt: HustleDebt, hustleName: String, settled: Long, onSettle: () -> Unit) {
    val remaining = (debt.originalAmountMinor - settled).coerceAtLeast(0L)
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (debt.direction == HustleDebt.CUSTOMER_OWES_ME) StudioGreenTint else StudioAmberTint), contentAlignment = Alignment.Center) {
                StudioIcon(if (debt.direction == HustleDebt.CUSTOMER_OWES_ME) "wallet" else "receipt",
                    if (debt.direction == HustleDebt.CUSTOMER_OWES_ME) StudioGreen else Color(0xFF9A6700), Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(debt.counterparty, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(hustleName + " · " + if (debt.direction == HustleDebt.CUSTOMER_OWES_ME) "Owes you" else "You owe",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(hustleKes(remaining), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(if (remaining == 0L) "SETTLED" else "OUTSTANDING", style = MaterialTheme.typography.labelSmall, color = if (remaining == 0L) StudioGreen else StudioMuted)
            }
        }
        if (remaining > 0L) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onSettle) { Text("Record repayment") } }
        HorizontalDivider(color = StudioLine)
    }
}

@Composable
private fun ReportLine(label: String, value: String, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(accent))
        Text(label, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickAction(label: String, detail: String, kind: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val accent = if (kind == "income") StudioGreen else TrackItExpense
    val tint = if (kind == "income") StudioGreenTint else StudioRedTint
    Surface(Modifier.then(modifier).clip(RoundedCornerShape(18.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, StudioLine)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint), contentAlignment = Alignment.Center) {
                StudioIcon(if (kind == "income") "income" else "expense", accent, Modifier.size(21.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SecondaryAction(label: String, icon: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(Modifier.then(modifier).clip(RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            StudioIcon(icon, MaterialTheme.colorScheme.onSurface, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, message: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(TrackItEmeraldTint), contentAlignment = Alignment.Center) { StudioIcon(icon, TrackItEmeraldDark, Modifier.size(25.dp)) }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        TextButton(onClick = onAction) { Text("Get started", color = TrackItEmeraldDark) }
    }
}

@Composable
private fun StudioIcon(kind: String, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sw = size.minDimension * 0.095f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.18f
        val bottom = size.height * 0.82f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, androidx.compose.ui.geometry.Offset(x1, y1), androidx.compose.ui.geometry.Offset(x2, y2), sw, StrokeCap.Round)
        }
        fun path(points: List<Pair<Float, Float>>) {
            val p = Path().apply { points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.first * size.width, point.second * size.height) else lineTo(point.first * size.width, point.second * size.height)
            } }
            drawPath(p, color, style = stroke)
        }
        when (kind) {
            "plus" -> { line(size.width * .5f, top, size.width * .5f, bottom); line(left, size.height * .5f, right, size.height * .5f) }
            "check" -> path(listOf(.2f to .52f, .42f to .72f, .82f to .27f))
            "trend" -> {
                line(left, bottom, left, top); line(left, bottom, right, bottom)
                path(listOf(.22f to .65f, .43f to .48f, .59f to .57f, .81f to .28f))
                line(size.width * .67f, size.height * .28f, size.width * .81f, size.height * .28f)
                line(size.width * .81f, size.height * .28f, size.width * .81f, size.height * .42f)
            }
            "briefcase" -> {
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .32f),
                    androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .49f), androidx.compose.ui.geometry.CornerRadius(size.width * .08f), style = stroke)
                path(listOf(.37f to .31f, .37f to .2f, .63f to .2f, .63f to .31f))
                line(size.width * .17f, size.height * .5f, size.width * .83f, size.height * .5f)
            }
            "wallet" -> {
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(size.width * .13f, size.height * .25f),
                    androidx.compose.ui.geometry.Size(size.width * .74f, size.height * .56f), androidx.compose.ui.geometry.CornerRadius(size.width * .09f), style = stroke)
                line(size.width * .58f, size.height * .46f, size.width * .87f, size.height * .46f)
                drawCircle(color, size.minDimension * .045f, androidx.compose.ui.geometry.Offset(size.width * .69f, size.height * .57f))
            }
            "receipt" -> {
                path(listOf(.27f to .15f, .73f to .15f, .73f to .83f, .63f to .76f, .5f to .83f, .37f to .76f, .27f to .83f, .27f to .15f))
                line(size.width * .37f, size.height * .36f, size.width * .63f, size.height * .36f)
                line(size.width * .37f, size.height * .51f, size.width * .63f, size.height * .51f)
                line(size.width * .37f, size.height * .65f, size.width * .55f, size.height * .65f)
            }
            "income" -> { line(size.width * .5f, bottom, size.width * .5f, top); path(listOf(.25f to .4f, .5f to .17f, .75f to .4f)); line(size.width * .2f, bottom, size.width * .8f, bottom) }
            "expense" -> { line(size.width * .5f, top, size.width * .5f, bottom); path(listOf(.25f to .6f, .5f to .83f, .75f to .6f)); line(size.width * .2f, top, size.width * .8f, top) }
            else -> drawCircle(color, size.minDimension * .28f, androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .5f), style = stroke)
        }
    }
}

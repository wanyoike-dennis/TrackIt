package com.inventsolutions.trackit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventsolutions.trackit.ui.theme.TrackItAmber
import com.inventsolutions.trackit.ui.theme.TrackItAmberTint
import com.inventsolutions.trackit.ui.theme.TrackItEmerald
import com.inventsolutions.trackit.ui.theme.TrackItEmeraldTint
import com.inventsolutions.trackit.ui.theme.TrackItExpense
import com.inventsolutions.trackit.ui.theme.TrackItExpenseTintLight
import com.inventsolutions.trackit.ui.theme.TrackItNavy
import com.inventsolutions.trackit.ui.theme.TrackItNavyElevated
import com.inventsolutions.trackit.ui.theme.TrackItNavySurface
import com.inventsolutions.trackit.ui.theme.TrackItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackItTheme {
                DashboardScreen()
            }
        }
    }
}

private data class ActivityItem(
    val title: String,
    val category: String,
    val amount: String,
    val income: Boolean
)

private val previewActivity = listOf(
    ActivityItem("Design gig", "Side hustle · Today", "+ KSh 2,500", true),
    ActivityItem("Lunch", "Food · Today", "− KSh 280", false),
    ActivityItem("Motorbike fare", "Transport · Yesterday", "− KSh 150", false)
)

@Composable
private fun DashboardScreen() {
    var selectedTab by remember { mutableStateOf("Home") }
    var notice by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("Home", "Activity", "Budgets", "More").forEach { tab ->
                        Column(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedTab = tab
                                    if (tab != "Home") notice = true
                                }.padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                when (tab) {
                                    "Home" -> "⌂"
                                    "Activity" -> "↕"
                                    "Budgets" -> "▤"
                                    else -> "•••"
                                },
                                color = if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 21.sp
                            )
                            Text(
                                tab,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("TrackIt", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(Modifier.height(3.dp))
                            Text("Your money, made clearer.", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            Modifier.size(46.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("T", style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                item { OverviewCard() }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Quick actions", "Keep your records up to date")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            QuickAction("＋", "Add income", TrackItEmeraldTint, TrackItEmerald,
                                Modifier.weight(1f)) { notice = true }
                            QuickAction("−", "Add expense", TrackItExpenseTintLight, TrackItExpense,
                                Modifier.weight(1f)) { notice = true }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Budget progress", "October spending plan", "View all")
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Overall budget", style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(3.dp))
                                        Text("KSh 13,580 of KSh 25,000 spent",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("54%", style = MaterialTheme.typography.titleLarge)
                                }
                                LinearProgressIndicator(
                                    progress = { 0.5432f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = TrackItEmerald,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(TrackItAmber))
                                    Spacer(Modifier.width(8.dp))
                                    Text("KSh 11,420 remaining", Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("11 days left", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MiniBudget("Food", "KSh 4,200", "of KSh 6,000", 0.70f,
                                TrackItEmerald, Modifier.weight(1f))
                            MiniBudget("Transport", "KSh 2,100", "of KSh 3,000", 0.70f,
                                TrackItAmber, Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Recent activity", "Your latest money movements", "See history")
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column {
                                previewActivity.forEachIndexed { index, item ->
                                    ActivityRow(item)
                                    if (index != previewActivity.lastIndex) {
                                        HorizontalDivider(Modifier.padding(start = 68.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Surface(color = TrackItAmberTint.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Dashboard preview", color = TrackItNavy,
                                style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("Figures and transactions are sample content. Live totals will come from your saved records once transaction tracking is connected.",
                                color = TrackItNavy, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (notice) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Transaction and budget screens are coming next.",
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { notice = false }) { Text("OK") }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewCard() {
    val isDark = MaterialTheme.colorScheme.background == TrackItBackgroundDark
    val cardShape = RoundedCornerShape(26.dp)
    val cardBackground = if (isDark) TrackItNavyElevated else TrackItNavy
    val monthPillBackground = if (isDark) TrackItNavySurface else TrackItNavyElevated

    Column(
        Modifier.fillMaxWidth()
            .clip(cardShape)
            .background(cardBackground)
            .then(
                if (isDark) Modifier.border(
                    width = 1.dp,
                    color = TrackItNavySurface,
                    shape = cardShape
                ) else Modifier
            )
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MONTHLY OVERVIEW", Modifier.weight(1f), color = Color(0xFFB8C2D1),
                style = MaterialTheme.typography.labelMedium, letterSpacing = 1.1.sp)
            Surface(color = monthPillBackground, shape = RoundedCornerShape(50)) {
                Text("OCTOBER 2026", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFFE5EAF1), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Money left this month", color = Color(0xFFD0D7E2),
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("KSh 18,420", color = Color.White,
            style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = TrackItNavySurface)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("↗  Income", color = Color(0xFF6EE7B7), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                Text("KSh 32,000", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text("↘  Spent", color = Color(0xFFFFA3A3), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                Text("KSh 13,580", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun QuickAction(
    symbol: String, title: String, tint: Color, accent: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(tint),
                contentAlignment = Alignment.Center) {
                Text(symbol, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
            Text(title, color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun MiniBudget(
    title: String, spent: String, budget: String, progress: Float,
    accent: Color, modifier: Modifier = Modifier
) {
    Card(modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(spent, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(budget, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = accent, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    val tint = if (item.income) TrackItEmeraldTint else TrackItExpenseTintLight
    val accent = if (item.income) TrackItEmerald else TrackItExpense
    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint),
            contentAlignment = Alignment.Center) {
            Text(if (item.income) "↗" else "↓", color = accent, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(item.category, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(item.amount, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (item.income) TrackItEmerald else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, action: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(name = "TrackIt Dashboard - Light", showBackground = true)
@Composable
private fun DashboardLightPreview() {
    TrackItTheme(darkTheme = false) { DashboardScreen() }
}

@Preview(name = "TrackIt Dashboard - Dark", showBackground = true)
@Composable
private fun DashboardDarkPreview() {
    TrackItTheme(darkTheme = true) { DashboardScreen() }
}

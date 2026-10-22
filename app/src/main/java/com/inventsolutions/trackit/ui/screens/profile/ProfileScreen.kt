package com.inventsolutions.trackit.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenBackupRestore: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Text("‹", style = MaterialTheme.typography.headlineMedium)
            }
            Column(Modifier.weight(1f)) {
                Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Account and app preferences", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text("D", modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Your TrackIt profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Manage your personal app settings.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Text("ACCOUNT & PREFERENCES", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProfileOption(
            title = "Personal information",
            description = "Your profile details",
            icon = { Text("●", color = MaterialTheme.colorScheme.primary) },
            onClick = {}
        )
        ProfileOption(
            title = "Appearance",
            description = "Theme and display preferences",
            icon = { Text("◐", color = MaterialTheme.colorScheme.primary) },
            onClick = {}
        )

        Spacer(Modifier.height(2.dp))
        Text("DATA & STORAGE", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProfileOption(
            title = "Backup & Restore",
            description = "Export or restore your financial records",
            icon = { Text("⇅", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge) },
            onClick = onOpenBackupRestore
        )

        Spacer(Modifier.height(2.dp))
        Text("SUPPORT", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ProfileOption(
            title = "Help & Support",
            description = "Get help using TrackIt",
            icon = { Text("?", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge) },
            onClick = {}
        )
    }
}

@Composable
private fun ProfileOption(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                androidx.compose.foundation.layout.Box(Modifier.padding(10.dp)) { icon() }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

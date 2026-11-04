package com.inventsolutions.trackit.ui.screens.profile

import com.inventsolutions.trackit.R
import com.inventsolutions.trackit.ui.components.TrackItIcon
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

private const val PROFILE_PREFS = "trackit_personal_profile"

@Composable
fun PersonalInformationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE) }
    var fullName by remember { mutableStateOf(preferences.getString("full_name", "").orEmpty()) }
    var email by remember { mutableStateOf(preferences.getString("email", "").orEmpty()) }
    var phone by remember { mutableStateOf(preferences.getString("phone", "").orEmpty()) }
    var location by remember { mutableStateOf(preferences.getString("location", "").orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { TrackItIcon(R.drawable.ic_lucide_arrow_left, null); Text("Back") }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Personal information", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text("Add the details you want TrackIt to display with your profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it; error = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("Full name") },
            placeholder = { Text("Enter your name") }, singleLine = true,
            supportingText = { Text("Required") }
        )
        OutlinedTextField(
            value = email, onValueChange = { email = it; error = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("Email address") },
            placeholder = { Text("name@example.com") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        OutlinedTextField(
            value = phone, onValueChange = { phone = it; error = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("Phone number") },
            placeholder = { Text("+254 7XX XXX XXX") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        OutlinedTextField(
            value = location, onValueChange = { location = it; error = null },
            modifier = Modifier.fillMaxWidth(), label = { Text("Location") },
            placeholder = { Text("Town or city") }, singleLine = true
        )

        Text(
            "These details are stored locally on this device. They are not used to sign in or verify an account.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                val normalizedEmail = email.trim()
                if (fullName.isBlank()) {
                    error = "Please enter your full name."
                } else if (normalizedEmail.isNotEmpty() &&
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
                ) {
                    error = "Enter a valid email address or leave it blank."
                } else {
                    preferences.edit()
                        .putString("full_name", fullName.trim())
                        .putString("email", normalizedEmail)
                        .putString("phone", phone.trim())
                        .putString("location", location.trim())
                        .apply()
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            TrackItIcon(R.drawable.ic_lucide_user, null, tint = MaterialTheme.colorScheme.onPrimary); Text("Save changes")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

package com.inventsolutions.trackit.ui.screens.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.inventsolutions.trackit.ui.theme.TrackItAmber
import com.inventsolutions.trackit.ui.theme.TrackItEmerald
import com.inventsolutions.trackit.ui.theme.TrackItNavy

private const val PROFILE_PREFS = "trackit_personal_profile"

@Composable
fun PersonalInformationScreen(onBack: () -> Unit, isInitialSetup: Boolean = false) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE) }
    var fullName by remember { mutableStateOf(preferences.getString("full_name", "").orEmpty()) }
    var email by remember { mutableStateOf(preferences.getString("email", "").orEmpty()) }
    var phone by remember { mutableStateOf(preferences.getString("phone", "").orEmpty()) }
    var location by remember { mutableStateOf(preferences.getString("location", "").orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun saveProfile() {
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
    }

    if (isInitialSetup) {
        InitialProfileSetup(
            fullName = fullName,
            onFullNameChange = { fullName = it; error = null },
            email = email,
            onEmailChange = { email = it; error = null },
            phone = phone,
            onPhoneChange = { phone = it; error = null },
            location = location,
            onLocationChange = { location = it; error = null },
            error = error,
            onSave = ::saveProfile
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("‹  Back") }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Personal information", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Add the details you want TrackIt to display with your profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ProfileField("Full name", fullName, { fullName = it; error = null }, "Enter your name")
            ProfileField("Email address", email, { email = it; error = null }, "name@example.com",
                keyboardType = KeyboardType.Email)
            ProfileField("Phone number", phone, { phone = it; error = null }, "+254 7XX XXX XXX",
                keyboardType = KeyboardType.Phone)
            ProfileField("Location", location, { location = it; error = null }, "Town or city")
            Text("These details are stored locally on this device. They are not used to sign in or verify an account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = ::saveProfile, modifier = Modifier.fillMaxWidth()) {
                Text("Save changes")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun InitialProfileSetup(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    error: String?,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = TrackItEmerald
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text("T", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("TrackIt", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("PERSONAL FINANCE", fontSize = 9.sp, letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = TrackItEmerald.copy(alpha = 0.10f)) {
                Text("WELCOME", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    color = TrackItEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(listOf(TrackItNavy, Color(0xFF174B40))),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(9.dp), color = TrackItAmber.copy(alpha = 0.16f)) {
                Text("LET’S GET YOU STARTED",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = TrackItAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp)
            }
            Text("Your money.
Your picture.", color = Color.White,
                fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.ExtraBold)
            Text("Set up your profile so your financial space feels like yours from day one.",
                color = Color(0xFFD0D9E2), style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(modifier = Modifier.size(28.dp), shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f)) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Profile details", color = Color.White, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium)
                    Text("One quick step before your dashboard",
                        color = Color(0xFFB8C7C7), fontSize = 11.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("A little about you", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Your name is the only required detail. Everything else is optional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            ProfileField("FULL NAME", fullName, onFullNameChange, "e.g. Dennis Njoroge")
            ProfileField("EMAIL ADDRESS", email, onEmailChange, "you@example.com",
                keyboardType = KeyboardType.Email)
            ProfileField("PHONE NUMBER", phone, onPhoneChange, "+254 7XX XXX XXX",
                keyboardType = KeyboardType.Phone)
            ProfileField("LOCATION", location, onLocationChange, "Town or city")
        }

        if (error != null) {
            Surface(shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer) {
                Text(error, modifier = Modifier.padding(13.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TrackItEmerald)
        ) {
            Text("Save profile and continue", fontWeight = FontWeight.Bold,
                fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            Text("→", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text("🔒", fontSize = 12.sp)
            Spacer(Modifier.width(7.dp))
            Text("Your profile details stay on this device.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, letterSpacing = 0.65.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)) },
            singleLine = true,
            shape = RoundedCornerShape(15.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TrackItEmerald,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

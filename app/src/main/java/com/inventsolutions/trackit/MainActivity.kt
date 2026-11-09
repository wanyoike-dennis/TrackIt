package com.inventsolutions.trackit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.inventsolutions.trackit.notifications.BudgetNotifications
import com.inventsolutions.trackit.notifications.DailyRecordReminder
import com.inventsolutions.trackit.ui.screens.dashboard.DashboardScreen
import com.inventsolutions.trackit.ui.screens.profile.PersonalInformationScreen
import com.inventsolutions.trackit.ui.theme.TrackItTheme
import com.inventsolutions.trackit.ui.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    private lateinit var transactionViewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BudgetNotifications.createChannel(this)
        DailyRecordReminder.scheduleNext(this)
        requestNotificationPermissionIfNeeded()
        transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        setContent {
            val appearancePreferences = getSharedPreferences("trackit_appearance", MODE_PRIVATE)
            var themeMode by remember {
                mutableStateOf(appearancePreferences.getString("theme_mode", "system") ?: "system")
            }
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val profilePreferences = getSharedPreferences("trackit_personal_profile", MODE_PRIVATE)
            var profileSetupRequired by remember {
                mutableStateOf(profilePreferences.getString("full_name", "").orEmpty().isBlank())
            }
            BackHandler(enabled = profileSetupRequired) { }
            TrackItTheme(darkTheme = darkTheme) {
                if (profileSetupRequired) {
                    PersonalInformationScreen(
                        isInitialSetup = true,
                        onBack = { profileSetupRequired = false }
                    )
                } else {
                    DashboardScreen(
                        viewModel = transactionViewModel,
                        themeMode = themeMode,
                        onThemeModeChanged = { mode ->
                            if (mode == "system" || mode == "light" || mode == "dark") {
                                appearancePreferences.edit().putString("theme_mode", mode).apply()
                                themeMode = mode
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 410
    }
}

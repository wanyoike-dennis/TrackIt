package com.inventsolutions.trackit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.inventsolutions.trackit.notifications.BudgetNotifications
import com.inventsolutions.trackit.ui.screens.dashboard.DashboardScreen
import com.inventsolutions.trackit.ui.theme.TrackItTheme
import com.inventsolutions.trackit.ui.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    private lateinit var transactionViewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BudgetNotifications.createChannel(this)
        requestNotificationPermissionIfNeeded()
        transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        setContent {
            TrackItTheme {
                DashboardScreen(transactionViewModel)
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

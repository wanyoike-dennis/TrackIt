package com.inventsolutions.trackit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.inventsolutions.trackit.ui.screens.dashboard.DashboardScreen
import com.inventsolutions.trackit.ui.theme.TrackItTheme
import com.inventsolutions.trackit.ui.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {
    private lateinit var transactionViewModel: TransactionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        transactionViewModel = ViewModelProvider(this)[TransactionViewModel::class.java]
        setContent {
            TrackItTheme {
                DashboardScreen(transactionViewModel)
            }
        }
    }
}

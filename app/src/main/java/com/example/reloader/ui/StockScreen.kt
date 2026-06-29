package com.example.reloader.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reloader.model.AppStatus
import com.example.reloader.model.AppUiState
import com.example.reloader.viewmodel.StockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(viewModel: StockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SIM Stock Checker", color = Color.White, fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StockCard(uiState)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.checkStock() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.status !is AppStatus.Running && uiState.status !is AppStatus.WaitingForSms,
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState.status is AppStatus.Running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Checking...")
                } else if (uiState.status is AppStatus.WaitingForSms) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Waiting for SMS...")
                } else {
                    Text("Check Stock", fontSize = 18.sp)
                }
            }

            StatusSection(uiState)

            if (!uiState.isAccessibilityEnabled) {
                PermissionBanner {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
    }
}

@Composable
fun StockCard(uiState: AppUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StockItem(label = "Available Balance", value = uiState.stockInfo.balance, valueColor = MaterialTheme.colorScheme.primary)
            StockItem(label = "Commission", value = uiState.stockInfo.commission, valueColor = MaterialTheme.colorScheme.secondary)
            StockItem(label = "Last Updated", value = uiState.stockInfo.lastUpdated)
        }
    }
}

@Composable
fun StockItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun StatusSection(uiState: AppUiState) {
    val statusText = when (val status = uiState.status) {
        is AppStatus.Idle -> "Ready"
        is AppStatus.Running -> "Running Automation..."
        is AppStatus.WaitingForSms -> "Waiting for SMS Response..."
        is AppStatus.Success -> "Success: ${status.message}"
        is AppStatus.Failed -> "Error: ${status.error}"
    }

    val statusColor = when (uiState.status) {
        is AppStatus.Success -> Color(0xFF4CAF50)
        is AppStatus.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Status:", style = MaterialTheme.typography.labelMedium)
        Text(text = statusText, style = MaterialTheme.typography.bodyLarge, color = statusColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PermissionBanner(onEnableClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Accessibility Service is required to automate SIM Toolkit.",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = onEnableClick) {
                Text("Enable Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

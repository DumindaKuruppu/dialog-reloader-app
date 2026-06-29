package com.example.reloader.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reloader.R
import com.example.reloader.model.AppStatus
import com.example.reloader.model.AppUiState
import com.example.reloader.model.ReloadAmount
import com.example.reloader.model.StockInfo
import com.example.reloader.ui.theme.ReloaderTheme
import com.example.reloader.viewmodel.StockViewModel

@Composable
fun StockScreen(viewModel: StockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    StockContent(
        uiState = uiState,
        onCheckStock = { viewModel.checkStock() },
        onPerformReload = { viewModel.performReload(it) },
        onSaveReload = { label, phone, amount -> viewModel.saveReload(label, phone, amount) },
        onDeleteReload = { viewModel.deleteReload(it) },
        onEnableAccessibility = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockContent(
    uiState: AppUiState,
    onCheckStock: () -> Unit,
    onPerformReload: (ReloadAmount) -> Unit,
    onSaveReload: (String, String, String) -> Unit,
    onDeleteReload: (String) -> Unit,
    onEnableAccessibility: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Dialog Logo",
                            modifier = Modifier.size(70.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reloader",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StockCard(uiState)

            Button(
                onClick = onCheckStock,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.status !is AppStatus.Running && uiState.status !is AppStatus.WaitingForSms,
                shape = MaterialTheme.shapes.medium
            ) {
                when (uiState.status) {
                    is AppStatus.Running -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Checking...")
                    }
                    is AppStatus.WaitingForSms -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Waiting for SMS...")
                    }
                    else -> {
                        Text("ශේෂය පරීක්ෂා කරන්න", fontSize = 18.sp)
                    }
                }
            }

            Text(
                "පාරිභෝගික රීලෝඩ්ස්",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Start),
                fontWeight = FontWeight.Bold
            )

            ReloadList(
                reloads = uiState.reloads,
                onPerformReload = onPerformReload,
                onDeleteReload = onDeleteReload,
                isEnabled = uiState.status !is AppStatus.Running && uiState.status !is AppStatus.WaitingForSms
            )

            StatusSection(uiState)

            if (!uiState.isAccessibilityEnabled) {
                PermissionBanner(onEnableAccessibility)
            }
        }
    }

    if (showAddDialog) {
        AddReloadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, phone, amount ->
                onSaveReload(label, phone, amount)
                showAddDialog = false
            }
        )
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
            StockItem(label = "RD Balance", value = uiState.stockInfo.balance, valueColor = MaterialTheme.colorScheme.primary)
            StockItem(label = "Commission Balance", value = uiState.stockInfo.commission, valueColor = MaterialTheme.colorScheme.secondary)
            StockItem(label = "Last Updated", value = uiState.stockInfo.lastUpdated)
        }
    }
}

@Composable
fun ReloadList(
    reloads: List<ReloadAmount>,
    onPerformReload: (ReloadAmount) -> Unit,
    onDeleteReload: (String) -> Unit,
    isEnabled: Boolean
) {
    if (reloads.isEmpty()) {
        Text("No saved reloads yet.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            reloads.forEach { reload ->
                ReloadItem(reload, onPerformReload, onDeleteReload, isEnabled)
            }
        }
    }
}

@Composable
fun ReloadItem(
    reload: ReloadAmount,
    onPerformReload: (ReloadAmount) -> Unit,
    onDeleteReload: (String) -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reload.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${reload.phoneNumber} | Rs. ${reload.amount}", style = MaterialTheme.typography.bodySmall)
            }

            Row {
                IconButton(onClick = { onDeleteReload(reload.id) }) {
                    Text("🗑️", fontSize = 20.sp)
                }
                Button(
                    onClick = { onPerformReload(reload) },
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("රීලෝඩ්", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AddReloadDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    val presetAmounts = listOf("50", "100", "200", "500", "1000")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer Reload") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Customer Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Text("Select or Enter Amount", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        FilterChip(
                            selected = amount == preset,
                            onClick = { amount = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Custom Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (label.isNotEmpty() && phone.isNotEmpty() && amount.isNotEmpty()) onConfirm(label, phone, amount) },
                enabled = label.isNotEmpty() && phone.isNotEmpty() && amount.isNotEmpty()
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

@Preview(showBackground = true)
@Composable
fun StockScreenPreview() {
    ReloaderTheme {
        StockContent(
            uiState = AppUiState(
                stockInfo = StockInfo("Rs. 12,500.00", "Rs. 860.00", "10:42 AM"),
                reloads = listOf(ReloadAmount(label = "Test User", phoneNumber = "0771234567", amount = "100")),
                status = AppStatus.Idle,
                isAccessibilityEnabled = true
            ),
            onCheckStock = {},
            onPerformReload = {},
            onSaveReload = { _, _, _ -> },
            onDeleteReload = {},
            onEnableAccessibility = {}
        )
    }
}

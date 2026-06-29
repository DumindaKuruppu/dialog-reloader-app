package com.example.reloader.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.reloader.model.PresetAmount
import com.example.reloader.model.ReloadAmount
import com.example.reloader.model.StockInfo
import com.example.reloader.ui.theme.ReloaderTheme
import com.example.reloader.viewmodel.StockViewModel
import kotlinx.coroutines.launch

enum class Screen {
    Main,
    ManageAmounts
}

@Composable
fun StockScreen(viewModel: StockViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Main) }

    when (currentScreen) {
        Screen.Main -> {
            StockContent(
                uiState = uiState,
                onCheckStock = { viewModel.checkStock() },
                onPerformReload = { viewModel.performReload(it) },
                onSaveReload = { label, phone, amount -> viewModel.saveReload(label, phone, amount) },
                onDeleteReload = { viewModel.deleteReload(it) },
                onEnableAccessibility = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                onQuickReload = { phone, amount -> viewModel.performQuickReload(phone, amount) },
                onNavigateToManageAmounts = { currentScreen = Screen.ManageAmounts }
            )
        }
        Screen.ManageAmounts -> {
            ManageAmountsScreen(
                amounts = uiState.presetAmounts,
                onAdd = { amount, desc -> viewModel.addPresetAmount(amount, desc) },
                onDelete = { viewModel.deletePresetAmount(it) },
                onBack = { currentScreen = Screen.Main }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockContent(
    uiState: AppUiState,
    onCheckStock: () -> Unit,
    onPerformReload: (ReloadAmount) -> Unit,
    onSaveReload: (String, String, String) -> Unit,
    onDeleteReload: (String) -> Unit,
    onEnableAccessibility: () -> Unit,
    onQuickReload: (String, String) -> Unit,
    onNavigateToManageAmounts: () -> Unit
) {
    var showQuickReloadDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        "Settings & Management",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Section: Check Stock Action
                    Button(
                        onClick = {
                            onCheckStock()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        enabled = uiState.status !is AppStatus.Running && uiState.status !is AppStatus.WaitingForSms
                    ) {
                        Text("ශේෂය පරීක්ෂා කරන්න (Check Stock)")
                    }

                    HorizontalDivider()

                    // Navigation link to Manage Amounts
                    NavigationDrawerItem(
                        label = { Text("Manage Preset Amounts") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigateToManageAmounts()
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = null) }
                    )

                    HorizontalDivider()

                    // Section: Manage Customers
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saved Customers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showAddCustomerDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Customer")
                            }
                        }
                        
                        ReloadList(
                            reloads = uiState.reloads,
                            onPerformReload = { 
                                onPerformReload(it)
                                scope.launch { drawerState.close() }
                            },
                            onDeleteReload = onDeleteReload,
                            isEnabled = uiState.status is AppStatus.Idle
                        )
                    }
                }
            }
        }
    ) {

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
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch { drawerState.open() }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickReloadDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Text("රීලෝඩ්", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(0.6f)) {
                    StockCard(uiState)
                }
                Box(modifier = Modifier.weight(0.4f)) {
                    StatusSection(uiState)
                }
            }

            if (!uiState.isAccessibilityEnabled) {
                PermissionBanner(onEnableAccessibility)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent eZ Reload Messages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                MessageList(messages = uiState.recentMessages)
            }
        }
    }
}

    if (showQuickReloadDialog) {
        QuickReloadDialog(
            presetAmounts = uiState.presetAmounts,
            onDismiss = { showQuickReloadDialog = false },
            onConfirm = { phone, amount ->
                onQuickReload(phone, amount)
                showQuickReloadDialog = false
            }
        )
    }

    if (showAddCustomerDialog) {
        AddReloadDialog(
            presetAmounts = uiState.presetAmounts,
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { label, phone, amount ->
                onSaveReload(label, phone, amount)
                showAddCustomerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAmountsScreen(
    amounts: List<PresetAmount>,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var newAmount by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Preset Amounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add New Preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = newAmount,
                        onValueChange = { if (it.length <= 5) newAmount = it },
                        label = { Text("Amount (Rs.)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Data, Voice") }
                    )
                    Button(
                        onClick = {
                            onAdd(newAmount, newDescription)
                            newAmount = ""
                            newDescription = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newAmount.isNotEmpty()
                    ) {
                        Text("Add Preset")
                    }
                }
            }

            HorizontalDivider()

            Text("Current Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (amounts.isEmpty()) {
                Text("No presets added yet.", color = Color.Gray)
            } else {
                amounts.forEach { preset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Rs. ${preset.amount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                if (preset.description.isNotEmpty()) {
                                    Text(preset.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { onDelete(preset.amount) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StockItem(label = "RD Balance", value = uiState.stockInfo.balance, valueColor = MaterialTheme.colorScheme.primary)
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
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
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
fun QuickReloadDialog(
    presetAmounts: List<PresetAmount>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Reload") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Text("Select or Enter Amount", style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        FilterChip(
                            selected = amount == preset.amount,
                            onClick = { amount = preset.amount },
                            label = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(preset.amount)
                                    if (preset.description.isNotEmpty()) {
                                        Text(preset.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (phone.isNotEmpty() && amount.isNotEmpty()) onConfirm(phone, amount) },
                enabled = phone.isNotEmpty() && amount.isNotEmpty()
            ) {
                Text("Perform Reload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddReloadDialog(
    presetAmounts: List<PresetAmount>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

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
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        FilterChip(
                            selected = amount == preset.amount,
                            onClick = { amount = preset.amount },
                            label = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(preset.amount)
                                    if (preset.description.isNotEmpty()) {
                                        Text(preset.description, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
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
fun MessageList(messages: List<com.example.reloader.model.SmsMessage>) {
    if (messages.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No messages from eZ Reload found.", color = Color.Gray)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            messages.forEach { msg ->
                MessageItem(msg)
            }
        }
    }
}

@Composable
fun MessageItem(message: com.example.reloader.model.SmsMessage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "eZ Reload",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        Text(text = "Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = statusText, style = MaterialTheme.typography.bodyMedium, color = statusColor, fontWeight = FontWeight.Bold)
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
            onEnableAccessibility = {},
            onQuickReload = { _, _ -> },
            onNavigateToManageAmounts = {}
        )
    }
}

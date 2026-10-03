package com.pratham.swalekhi.ui.settings

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratham.swalekhi.util.BiometricAuth
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.util.CurrencyFormatter

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenCategories: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenRecurring: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showReminderTimeDialog by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onExport(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onImport(context, it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }

        item { SettingsSectionLabel("Money") }
        item {
            SettingsRow(
                icon = Icons.Filled.Savings,
                title = "Monthly budget",
                subtitle = uiState.budget?.let { CurrencyFormatter.format(it, uiState.currencyCode) } ?: "Not set",
                onClick = { showBudgetDialog = true }
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.CurrencyExchange,
                title = "Currency",
                subtitle = Currencies.byCode(uiState.currencyCode).displayName,
                onClick = { showCurrencyDialog = true }
            )
        }

        item { SettingsSectionLabel("Organize") }
        item { SettingsRow(icon = Icons.Filled.Category, title = "Categories", onClick = onOpenCategories) }
        item { SettingsRow(icon = Icons.Filled.AccountBalanceWallet, title = "Accounts", onClick = onOpenAccounts) }
        item { SettingsRow(icon = Icons.Filled.Repeat, title = "Recurring transactions", onClick = onOpenRecurring) }

        item { SettingsSectionLabel("Security & reminders") }
        item {
            SettingsToggleRow(
                icon = Icons.Filled.Fingerprint,
                title = "App lock",
                subtitle = "Require your fingerprint or PIN to open Swalekhi",
                checked = uiState.appLockEnabled,
                onCheckedChange = { enabled ->
                    viewModel.onAppLockToggle(enabled, BiometricAuth.canAuthenticate(context))
                }
            )
        }
        item {
            SettingsToggleRow(
                icon = Icons.Filled.NotificationsActive,
                title = "Daily reminder",
                subtitle = if (uiState.dailyReminderEnabled) {
                    "At %02d:%02d — tap to change".format(uiState.reminderHour, uiState.reminderMinute)
                } else {
                    "Off"
                },
                checked = uiState.dailyReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.onReminderToggle(context, enabled)
                },
                onClick = { if (uiState.dailyReminderEnabled) showReminderTimeDialog = true }
            )
        }

        item { SettingsSectionLabel("Data") }
        item {
            SettingsRow(
                icon = Icons.Filled.Upload,
                title = "Export data (CSV)",
                onClick = { exportLauncher.launch("swalekhi_backup.csv") }
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Download,
                title = "Import data (CSV)",
                onClick = { importLauncher.launch(arrayOf("*/*")) }
            )
        }
        item {
            SettingsRow(
                icon = Icons.Filled.Share,
                title = "Share this month's report",
                onClick = { viewModel.onShareReport(context) }
            )
        }
    }

    if (showBudgetDialog) {
        AmountDialog(
            title = "Monthly budget",
            description = "Set a target to compare your projected spend against. Leave empty to remove it.",
            currentValue = uiState.budget,
            onDismiss = { showBudgetDialog = false },
            onSave = { viewModel.onSaveBudget(it); showBudgetDialog = false }
        )
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            selectedCode = uiState.currencyCode,
            onDismiss = { showCurrencyDialog = false },
            onSelect = { viewModel.onCurrencySelected(it); showCurrencyDialog = false }
        )
    }

    if (showReminderTimeDialog) {
        TimePickerDialogSimple(
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onDismiss = { showReminderTimeDialog = false },
            onConfirm = { h, m -> viewModel.onReminderTimeChange(context, h, m); showReminderTimeDialog = false }
        )
    }

    uiState.statusMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onDismissMessage,
            title = { Text("Swalekhi") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::onDismissMessage) { Text("OK") } }
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AmountDialog(
    title: String,
    description: String,
    currentValue: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var text by remember {
        mutableStateOf(currentValue?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.toDoubleOrNull()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CurrencyPickerDialog(selectedCode: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            Column(modifier = Modifier.height(420.dp)) {
                LazyColumn {
                    items(Currencies.all) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option.code) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option.code == selectedCode, onClick = { onSelect(option.code) })
                            Text(text = "${option.symbol}  ${option.displayName}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun TimePickerDialogSimple(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Stepper(value = hour, range = 0..23, onChange = { hour = it })
                Text(text = " : ", style = MaterialTheme.typography.headlineSmall)
                Stepper(value = minute, range = 0..59, step = 5, onChange = { minute = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hour, minute) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Stepper(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = {
            val next = value + step
            onChange(if (next > range.last) range.first else next)
        }) { Text("+") }
        Text(text = "%02d".format(value), style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = {
            val prev = value - step
            onChange(if (prev < range.first) range.last else prev)
        }) { Text("-") }
    }
}

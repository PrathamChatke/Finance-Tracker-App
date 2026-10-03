package com.pratham.swalekhi.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pratham.swalekhi.data.RecurrenceFrequency
import com.pratham.swalekhi.ui.components.CategoryBadge
import com.pratham.swalekhi.ui.components.CategoryIcons
import com.pratham.swalekhi.ui.components.parseHexColor
import com.pratham.swalekhi.util.Currencies
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringScreen(viewModel: AddRecurringViewModel, onDone: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit recurring rule" else "New recurring rule") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = !uiState.isIncome, onClick = { viewModel.onTypeChange(false) }, label = { Text("Expense") }, modifier = Modifier.weight(1f))
                FilterChip(selected = uiState.isIncome, onClick = { viewModel.onTypeChange(true) }, label = { Text("Income") }, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = uiState.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount") },
                prefix = { Text(Currencies.byCode(uiState.currencyCode).symbol) },
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note (e.g. Rent, Netflix)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Repeats", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                FilterChip(
                    selected = uiState.frequency == RecurrenceFrequency.MONTHLY,
                    onClick = { viewModel.onFrequencyChange(RecurrenceFrequency.MONTHLY) },
                    label = { Text("Monthly") }
                )
                FilterChip(
                    selected = uiState.frequency == RecurrenceFrequency.WEEKLY,
                    onClick = { viewModel.onFrequencyChange(RecurrenceFrequency.WEEKLY) },
                    label = { Text("Weekly") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.frequency == RecurrenceFrequency.MONTHLY) {
                OutlinedTextField(
                    value = uiState.dayOfPeriod.toString(),
                    onValueChange = { text -> text.toIntOrNull()?.let { viewModel.onDayOfPeriodChange(it.coerceIn(1, 31)) } },
                    label = { Text("Day of month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((1..7).toList()) { dow ->
                        val label = DayOfWeek.of(dow).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        FilterChip(
                            selected = uiState.dayOfPeriod == dow,
                            onClick = { viewModel.onDayOfPeriodChange(dow) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (uiState.accounts.size > 1) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Account", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items(uiState.accounts) { account ->
                        FilterChip(
                            selected = account.id == uiState.selectedAccountId,
                            onClick = { viewModel.onAccountSelected(account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Category", style = MaterialTheme.typography.titleMedium)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 76.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)
            ) {
                items(uiState.categories) { category ->
                    val selected = category.id == uiState.selectedCategoryId
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = CircleShape,
                            color = if (selected) parseHexColor(category.colorHex).copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent
                        ) {
                            IconButton(onClick = { viewModel.onCategorySelected(category.id) }) {
                                CategoryBadge(icon = CategoryIcons.iconFor(category.iconKey), color = parseHexColor(category.colorHex))
                            }
                        }
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Button(onClick = viewModel::onSave, modifier = Modifier.fillMaxWidth()) {
                Text(if (uiState.isEditing) "Save changes" else "Create rule")
            }
        }
    }
}

package com.pratham.swalekhi.ui.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratham.swalekhi.prediction.SpendingPredictor
import com.pratham.swalekhi.ui.components.CategoryBadge
import com.pratham.swalekhi.ui.components.CategoryIcons
import com.pratham.swalekhi.ui.components.DonutChart
import com.pratham.swalekhi.ui.components.DonutSlice
import com.pratham.swalekhi.ui.components.SectionHeader
import com.pratham.swalekhi.ui.components.StatCard
import com.pratham.swalekhi.ui.components.parseHexColor
import com.pratham.swalekhi.util.CurrencyFormatter

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val showBudgetDialog by viewModel.showBudgetDialog.collectAsState()
    val currencyCode = uiState.currencyCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Keeps the header clear of the status bar and any notch/punch-hole/teardrop cutout,
            // and off the screen edges in landscape where a cutout can land on the side.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Swalekhi", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = viewModel::onEditBudgetClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Set monthly budget")
            }
        }

        MonthSelector(
            monthLabel = uiState.monthLabel,
            canGoNext = uiState.canGoToNextMonth,
            onPrevious = viewModel::onPreviousMonth,
            onNext = viewModel::onNextMonth
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val projection = uiState.projection
            if (projection != null) {
                item { ProjectionHeroCard(projection, currencyCode) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(
                            label = "Spent so far",
                            value = CurrencyFormatter.format(projection.spentSoFar, currencyCode),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Daily average",
                            value = CurrencyFormatter.format(projection.dailyAverage, currencyCode),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (projection.budget != null) {
                    item { BudgetCard(projection, currencyCode) }
                }
            }

            item { IncomeExpenseNetCard(uiState.incomeTotal, uiState.expenseTotal, uiState.netTotal, currencyCode) }

            if (uiState.categoryBudgets.isNotEmpty()) {
                item { SectionHeader(title = "Category budgets", modifier = Modifier.padding(top = 4.dp)) }
                items(uiState.categoryBudgets) { CategoryBudgetRow(it, currencyCode) }
            }

            if (uiState.breakdown.isNotEmpty()) {
                item { SectionHeader(title = "Where it's going", modifier = Modifier.padding(top = 4.dp)) }
                item { CategoryBreakdownCard(uiState.breakdown, currencyCode) }
            } else if (!uiState.isLoading) {
                item {
                    Text(
                        text = "No expenses recorded for this month. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = uiState.projection?.budget,
            onDismiss = viewModel::onDismissBudgetDialog,
            onSave = viewModel::onSaveBudget
        )
    }
}

@Composable
private fun MonthSelector(
    monthLabel: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Next month",
                tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ProjectionHeroCard(projection: SpendingPredictor.Projection, currencyCode: String) {
    val overBudget = projection.projectedOverBudget
    val accent = when {
        overBudget == null -> MaterialTheme.colorScheme.primary
        overBudget > 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = 0.10f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Projected month-end spend",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.format(projection.projectedTotal, currencyCode),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = "Based on ${CurrencyFormatter.format(projection.dailyAverage, currencyCode)}/day over " +
                    "${projection.daysElapsed} of ${projection.totalDaysInMonth} days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (overBudget != null) {
                val message = if (overBudget > 0) {
                    "That's ${CurrencyFormatter.format(overBudget, currencyCode)} over your budget if this pace continues"
                } else {
                    "You're on track to stay ${CurrencyFormatter.format(-overBudget, currencyCode)} under budget"
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseNetCard(income: Double, expense: Double, net: Double, currencyCode: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IncomeExpenseStat("Income", income, currencyCode, MaterialTheme.colorScheme.primary)
            IncomeExpenseStat("Expense", expense, currencyCode, MaterialTheme.colorScheme.error)
            IncomeExpenseStat("Net", net, currencyCode, if (net >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun IncomeExpenseStat(label: String, amount: Double, currencyCode: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = CurrencyFormatter.formatSigned(amount, currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CategoryBudgetRow(item: CategoryBudgetProgress, currencyCode: String) {
    val progress = (item.spent / item.budget).toFloat().coerceIn(0f, 1f)
    val over = item.spent > item.budget
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(category = item.category, size = 28.dp)
                    Text(text = item.category.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    text = "${CurrencyFormatter.format(item.spent, currencyCode)} / ${CurrencyFormatter.format(item.budget, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(6.dp),
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetCard(projection: SpendingPredictor.Projection, currencyCode: String) {
    val progress = (projection.budgetProgress ?: 0f).coerceIn(0f, 1f)
    val over = (projection.spentSoFar > (projection.budget ?: Double.MAX_VALUE))
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Monthly budget", style = MaterialTheme.typography.labelLarge)
                Text(text = CurrencyFormatter.format(projection.budget ?: 0.0, currencyCode), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            projection.safeToSpendPerDay?.let { safe ->
                Text(
                    text = if (safe >= 0) {
                        "Safe to spend ${CurrencyFormatter.format(safe, currencyCode)}/day for the rest of the month"
                    } else {
                        "Already over budget by ${CurrencyFormatter.format(-safe * projection.daysRemaining.coerceAtLeast(1), currencyCode)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(items: List<CategoryBreakdownItem>, currencyCode: String) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val slices = items.map { item ->
                val color = item.category?.let { parseHexColor(it.colorHex) } ?: Color.Gray
                DonutSlice(value = item.total, color = color)
            }
            DonutChart(slices = slices, modifier = Modifier.size(120.dp))

            Column(modifier = Modifier.padding(start = 16.dp)) {
                items.take(5).forEach { item ->
                    LegendRow(item, currencyCode)
                }
                if (items.size > 5) {
                    Text(
                        text = "+${items.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendRow(item: CategoryBreakdownItem, currencyCode: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        val color = item.category?.let { parseHexColor(it.colorHex) } ?: Color.Gray
        CategoryBadge(
            icon = item.category?.let { CategoryIcons.iconFor(it.iconKey) } ?: CategoryIcons.iconFor("other"),
            color = color,
            size = 24.dp
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = item.category?.name ?: "Uncategorized",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CurrencyFormatter.format(item.total, currencyCode) + "  ·  ${(item.fraction * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetDialog(
    currentBudget: Double?,
    onDismiss: () -> Unit,
    onSave: (Double?) -> Unit
) {
    var text by remember { mutableStateOf(currentBudget?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget") },
        text = {
            Column {
                Text(
                    text = "Set a target to compare your projected spend against. Leave empty to remove it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter { it.isDigit() || it == '.' } },
                    label = { Text("Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toDoubleOrNull()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

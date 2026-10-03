package com.pratham.swalekhi.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratham.swalekhi.ui.components.BarChart
import com.pratham.swalekhi.ui.components.BarChartEntry
import com.pratham.swalekhi.ui.components.SectionHeader
import com.pratham.swalekhi.util.CurrencyFormatter
import com.pratham.swalekhi.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(text = "Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        item {
            SectionHeader(title = "Last 6 months")
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val total = uiState.monthlyTotals.sumOf { it.total }
                    Text(
                        text = "${CurrencyFormatter.format(total, uiState.currencyCode)} total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    BarChart(
                        entries = uiState.monthlyTotals.mapIndexed { index, monthTotal ->
                            BarChartEntry(
                                label = DateUtils.formatShortMonthLabel(monthTotal.yearMonth).substringBefore(" "),
                                value = monthTotal.total,
                                highlighted = index == uiState.monthlyTotals.lastIndex
                            )
                        }
                    )
                }
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item { SectionHeader(title = "By category") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = category.id == uiState.selectedCategoryId,
                            onClick = { viewModel.onCategorySelected(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val total = uiState.selectedCategoryTotals.sumOf { it.total }
                        Text(
                            text = "${CurrencyFormatter.format(total, uiState.currencyCode)} over 6 months",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        BarChart(
                            entries = uiState.selectedCategoryTotals.mapIndexed { index, monthTotal ->
                                BarChartEntry(
                                    label = DateUtils.formatShortMonthLabel(monthTotal.yearMonth).substringBefore(" "),
                                    value = monthTotal.total,
                                    highlighted = index == uiState.selectedCategoryTotals.lastIndex
                                )
                            },
                            barColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

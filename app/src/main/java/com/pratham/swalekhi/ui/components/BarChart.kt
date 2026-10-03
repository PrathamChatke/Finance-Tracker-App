package com.pratham.swalekhi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class BarChartEntry(val label: String, val value: Double, val highlighted: Boolean = false)

/** Minimal vertical bar chart drawn with plain Canvas — one bar per entry, labeled below. */
@Composable
fun BarChart(
    entries: List<BarChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.error,
    height: androidx.compose.ui.unit.Dp = 140.dp
) {
    val maxValue = (entries.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            if (entries.isEmpty()) return@Canvas
            val slotWidth = size.width / entries.size
            val barWidth = slotWidth * 0.5f
            entries.forEachIndexed { index, entry ->
                val barHeight = (entry.value / maxValue * size.height).toFloat().coerceAtLeast(2f)
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                drawRect(
                    color = if (entry.highlighted) highlightColor else barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight)
                )
            }
        }
        androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

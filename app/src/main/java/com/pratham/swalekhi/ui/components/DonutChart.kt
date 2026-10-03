package com.pratham.swalekhi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class DonutSlice(val value: Double, val color: Color)

/**
 * Minimal ring chart drawn with plain Canvas arcs — avoids pulling in a charting
 * library for what is a handful of category proportions.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: androidx.compose.ui.unit.Dp = 22.dp,
    trackColor: Color = Color.LightGray.copy(alpha = 0.25f),
    center: @Composable () -> Unit = {}
) {
    val total = slices.sumOf { it.value }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
            )

            if (total > 0) {
                var startAngle = -90f
                for (slice in slices) {
                    val sweep = (slice.value / total * 360.0).toFloat()
                    if (sweep <= 0f) continue
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
        }
        center()
    }
}

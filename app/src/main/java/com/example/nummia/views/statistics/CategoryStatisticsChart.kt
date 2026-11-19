package com.example.nummia.views.statistics

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import com.example.nummia.views.components.formatMoney

/** Serie del gráfico; los puntos nulos dejan huecos en vez de representar importes cero. */
internal data class ChartLine(
    val label: String,
    val color: Color,
    val points: List<Long?>
)

/** Dibuja una o varias series y comunica el índice del punto tocado al llamador. */
@Composable
internal fun SpendingLineChart(
    lines: List<ChartLine>,
    labels: List<String>,
    currencyCode: String,
    onSelectionChange: (Int) -> Unit = {}
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = lines.firstOrNull()?.color ?: MaterialTheme.colorScheme.primary
    val selectedIndex = remember(lines, labels) { mutableIntStateOf(-1) }
    val chartSize = remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val leftInset = with(density) { 76.dp.toPx() }
    val rightInset = with(density) { 10.dp.toPx() }
    val tooltipBackground = MaterialTheme.colorScheme.inverseSurface
    val tooltipForeground = MaterialTheme.colorScheme.inverseOnSurface

    Canvas(
        Modifier.fillMaxWidth()
            .height(220.dp)
            .onSizeChanged { chartSize.value = it }
            .pointerInput(lines, labels, chartSize.value) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateSelection(x: Float) {
                        val usableWidth = chartSize.value.width - leftInset - rightInset
                        if (labels.isNotEmpty() && usableWidth > 0f) {
                            val fraction = ((x - leftInset) / usableWidth).coerceIn(0f, 1f)
                            val index = (fraction * (labels.size - 1)).toInt().coerceIn(labels.indices)
                            selectedIndex.intValue = index
                            onSelectionChange(index)
                        }
                    }
                    updateSelection(down.position.x)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        event.changes.firstOrNull()?.let { updateSelection(it.position.x) }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        if (labels.isEmpty()) return@Canvas
        val left = 76.dp.toPx()
        val right = 10.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = 28.dp.toPx()
        val graphWidth = (size.width - left - right).coerceAtLeast(1f)
        val graphHeight = (size.height - top - bottom).coerceAtLeast(1f)
        val graphBottom = top + graphHeight
        val maxAmount = lines.flatMap { it.points }.filterNotNull().maxOrNull()?.coerceAtLeast(1L) ?: 1L
        val tickStep = maxAmount / 3.0
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.RIGHT
        }
        val nativeCanvas = drawContext.canvas.nativeCanvas

        for (tick in 0..3) {
            val fraction = tick / 3f
            val y = graphBottom - fraction * graphHeight
            drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, y),
                androidx.compose.ui.geometry.Offset(size.width - right, y), 1.dp.toPx())
            val value = (tickStep * tick).toLong()
            nativeCanvas.drawText(
                formatMoney(value, currencyCode),
                left - 8.dp.toPx(),
                y + 4.dp.toPx(),
                textPaint
            )
        }

        val xStep = if (labels.size <= 1) 0f else graphWidth / (labels.size - 1)
        if (selectedIndex.intValue in labels.indices) {
            val selectedX = left + selectedIndex.intValue * xStep
            drawLine(
                color = labelColor.copy(alpha = 0.65f),
                start = Offset(selectedX, top),
                end = Offset(selectedX, graphBottom),
                strokeWidth = 1.dp.toPx()
            )
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
        }
        val labelInterval = (labels.size / 5).coerceAtLeast(1)
        labels.forEachIndexed { index, label ->
            if (index % labelInterval == 0 || index == labels.lastIndex) {
                val x = left + index * xStep
                nativeCanvas.drawText(label, x, size.height - 4.dp.toPx(), labelPaint)
            }
        }

        lines.forEachIndexed { lineIndex, line ->
            val path = Path()
            var firstIndex: Int? = null
            var lastIndex: Int? = null
            line.points.forEachIndexed { index, amount ->
                if (amount != null) {
                    val x = left + index * xStep
                    val y = graphBottom - (amount.toDouble() / maxAmount * graphHeight).toFloat()
                    if (firstIndex == null) {
                        path.moveTo(x, y)
                        firstIndex = index
                    } else {
                        path.lineTo(x, y)
                    }
                    lastIndex = index
                }
            }
            if (firstIndex != null && lastIndex != null) {
                val startIndex = firstIndex ?: return@forEachIndexed
                val endIndex = lastIndex ?: return@forEachIndexed
                if (lineIndex == 0) {
                    val area = Path().apply {
                        addPath(path)
                        lineTo(left + endIndex * xStep, graphBottom)
                        lineTo(left + startIndex * xStep, graphBottom)
                        close()
                    }
                    drawPath(
                        area,
                        Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.01f)),
                            startY = top,
                            endY = graphBottom
                        )
                    )
                }
                drawPath(
                    path,
                    color = line.color,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                val endAmount = line.points[endIndex]
                if (endAmount != null) {
                    val endX = left + endIndex * xStep
                    val endY = graphBottom - (endAmount.toDouble() / maxAmount * graphHeight).toFloat()
                    drawCircle(line.color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(endX, endY))
                }
                val selectedAmount = line.points.getOrNull(selectedIndex.intValue)
                if (selectedAmount != null) {
                    val selectedX = left + selectedIndex.intValue * xStep
                    val selectedY =
                        graphBottom - (selectedAmount.toDouble() / maxAmount * graphHeight).toFloat()
                    drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(selectedX, selectedY))
                    drawCircle(line.color, radius = 4.dp.toPx(), center = Offset(selectedX, selectedY))
                }
            }
        }

        if (selectedIndex.intValue in labels.indices) {
            val index = selectedIndex.intValue
            val anchorAmount = lines.firstNotNullOfOrNull { it.points.getOrNull(index) }
            if (anchorAmount != null) {
                val anchorX = left + index * xStep
                val anchorY = graphBottom - (anchorAmount.toDouble() / maxAmount * graphHeight).toFloat()
                drawChartTooltip(
                    anchor = Offset(anchorX, anchorY),
                    label = labels[index],
                    lines = lines,
                    index = index,
                    currencyCode = currencyCode,
                    tooltipBackground = tooltipBackground,
                    tooltipForeground = tooltipForeground
                )
            }
        }
    }
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt()
)

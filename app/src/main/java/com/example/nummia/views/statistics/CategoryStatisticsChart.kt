package com.example.nummia.views.statistics

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.expenseColor
import com.example.nummia.views.components.formatMoney

/** Serie del gráfico; los puntos nulos dejan huecos en vez de representar importes cero. */
internal data class ChartLine(
    val label: String,
    val color: Color,
    val points: List<Long?>
)

@Composable
internal fun ComparisonTotal(
    label: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.padding(end = 6.dp)
                        .height(8.dp)
                        .width(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(color)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                formatMoney(amount, LocalCurrencyCode.current),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

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
                val tooltipTextSize = 12.sp.toPx()
                val tooltipLineHeight = 17.sp.toPx()
                val horizontalPadding = 12.dp.toPx()
                val verticalPadding = 9.dp.toPx()
                val swatchSize = 6.dp.toPx()
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = tooltipForeground.toArgb()
                    textSize = tooltipTextSize
                    typeface = android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT,
                        android.graphics.Typeface.BOLD
                    )
                }
                val tooltipRows = buildList {
                    add(labels[index] to null)
                    lines.forEach { line ->
                        line.points.getOrNull(index)?.let { amount ->
                            add("${line.label}: ${formatMoney(amount, currencyCode)}" to line.color)
                        }
                    }
                }
                val contentWidth = tooltipRows.maxOfOrNull { textPaint.measureText(it.first) } ?: 0f
                val tooltipWidth = (contentWidth + horizontalPadding * 2 + swatchSize)
                    .coerceAtMost(size.width - 16.dp.toPx())
                val tooltipHeight = tooltipRows.size * tooltipLineHeight + verticalPadding * 2
                val tooltipLeft = (anchorX - tooltipWidth / 2)
                    .coerceIn(8.dp.toPx(), size.width - tooltipWidth - 8.dp.toPx())
                val aboveTop = anchorY - tooltipHeight - 12.dp.toPx()
                val tooltipTop = if (aboveTop >= 4.dp.toPx()) {
                    aboveTop
                } else {
                    (anchorY + 12.dp.toPx()).coerceAtMost(size.height - tooltipHeight - 4.dp.toPx())
                }
                val canvas = drawContext.canvas.nativeCanvas
                val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = tooltipBackground.toArgb()
                    style = Paint.Style.FILL
                    setShadowLayer(5.dp.toPx(), 0f, 2.dp.toPx(), android.graphics.Color.BLACK)
                }
                canvas.save()
                canvas.drawRoundRect(
                    tooltipLeft,
                    tooltipTop,
                    tooltipLeft + tooltipWidth,
                    tooltipTop + tooltipHeight,
                    12.dp.toPx(),
                    12.dp.toPx(),
                    backgroundPaint
                )
                backgroundPaint.clearShadowLayer()
                tooltipRows.forEachIndexed { rowIndex, (text, seriesColor) ->
                    val baseline = tooltipTop + verticalPadding + (rowIndex + 1) * tooltipLineHeight -
                        (tooltipLineHeight - tooltipTextSize) / 2
                    textPaint.color = tooltipForeground.toArgb()
                    if (seriesColor != null) {
                        val swatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = seriesColor.toArgb()
                            style = Paint.Style.FILL
                        }
                        canvas.drawCircle(
                            tooltipLeft + horizontalPadding + swatchSize / 2,
                            baseline - tooltipTextSize / 3,
                            swatchSize / 2,
                            swatchPaint
                        )
                    }
                    canvas.drawText(
                        text,
                        tooltipLeft + horizontalPadding + swatchSize,
                        baseline,
                        textPaint
                    )
                }
                canvas.restore()
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

package com.example.nummia.views.statistics

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nummia.views.components.formatMoney

/** Dibuja la etiqueta flotante del punto seleccionado en el gráfico. */
internal fun DrawScope.drawChartTooltip(
    anchor: Offset,
    label: String,
    lines: List<ChartLine>,
    index: Int,
    currencyCode: String,
    tooltipBackground: Color,
    tooltipForeground: Color
) {
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
        add(label to null)
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
    val tooltipLeft = (anchor.x - tooltipWidth / 2)
        .coerceIn(8.dp.toPx(), size.width - tooltipWidth - 8.dp.toPx())
    val aboveTop = anchor.y - tooltipHeight - 12.dp.toPx()
    val tooltipTop = if (aboveTop >= 4.dp.toPx()) {
        aboveTop
    } else {
        (anchor.y + 12.dp.toPx()).coerceAtMost(
            size.height - tooltipHeight - 4.dp.toPx()
        )
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

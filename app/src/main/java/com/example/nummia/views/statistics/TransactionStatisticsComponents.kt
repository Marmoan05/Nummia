package com.example.nummia.views.statistics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.calculations.CategoryChartPeriod
import com.example.nummia.calculations.CategoryMonthComparison
import com.example.nummia.calculations.SpendingPoint
import com.example.nummia.calculations.StatisticsCategoryTotal
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.formatMonthHeader
import com.example.nummia.views.components.incomeColor
import com.example.nummia.views.components.expenseColor

/** Presenta la serie del periodo, su total y el desglose de categorías seleccionado. */
@Composable
internal fun TransactionPeriodSummaryCard(
    title: String,
    period: CategoryChartPeriod,
    cumulative: Boolean,
    combined: Boolean,
    shownIncome: Long,
    shownExpenses: Long,
    shownTotal: Long,
    currencyCode: String,
    chartLines: List<ChartLine>,
    chartLabels: List<String>,
    chartSeries: List<SpendingPoint>,
    selectedPoint: Int,
    breakdown: List<StatisticsCategoryTotal>,
    onCumulativeChange: (Boolean) -> Unit,
    onSelectionChange: (Int) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title.uppercase(), style = MaterialTheme.typography.labelMedium)
                    Text(period.label, style = MaterialTheme.typography.bodyMedium)
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        if (cumulative) "ACUMULADO" else "POR PERIODO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (combined) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ComparisonTotal(
                        label = "Ingresos",
                        amount = shownIncome,
                        color = incomeColor,
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonTotal(
                        label = "Gastos",
                        amount = shownExpenses,
                        color = expenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    formatMoney(shownTotal, currencyCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Acumular", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (cumulative) "Total desde el inicio del periodo hasta el punto elegido"
                        else "Solo el importe del punto elegido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.Switch(
                    checked = cumulative,
                    onCheckedChange = onCumulativeChange
                )
            }
            SpendingLineChart(
                lines = chartLines,
                labels = chartLabels,
                currencyCode = currencyCode,
                onSelectionChange = onSelectionChange
            )
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (selectedPoint >= 0 && selectedPoint in chartSeries.indices) {
                            "Categorías · ${chartSeries[selectedPoint].label}"
                        } else {
                            "Categorías · ${period.label.lowercase()}"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (cumulative && selectedPoint >= 0) {
                            "Acumulado hasta el punto marcado"
                        } else if (selectedPoint >= 0) {
                            "Desglose del punto marcado"
                        } else {
                            "Porcentaje sobre el total del periodo"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    formatMoney(shownTotal, currencyCode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (breakdown.isEmpty()) {
                Text(
                    "No hay movimientos en este periodo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                breakdown.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                onCategoryClick(item.category)
                            }
                        ) {
                            Text(item.category, modifier = Modifier.weight(1f))
                            Text("${(item.percentage * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(10.dp))
                            Text(formatMoney(item.amountCents, currencyCode))
                        }
                        LinearProgressIndicator(
                            progress = { item.percentage },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Compara el acumulado diario del mes actual con el mes seleccionado. */
@Composable
internal fun TransactionMonthComparisonCard(
    comparisonMonths: List<Long>,
    selectedMonthLabel: String,
    comparison: CategoryMonthComparison,
    comparisonMenuExpanded: Boolean,
    color: androidx.compose.ui.graphics.Color,
    currencyCode: String,
    onComparisonMonthChange: (Long) -> Unit,
    onComparisonMenuExpandedChange: (Boolean) -> Unit
) {
    val currentPoints = comparison.currentMonthPoints.map { it.amountCents }
    val priorPoints = comparison.comparisonMonthPoints.map { it.amountCents }
    val pointCount = maxOf(currentPoints.size, priorPoints.size)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Comparativa mensual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Compara el acumulado de este mes con otro mes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { onComparisonMenuExpandedChange(true) }) {
                    Text(selectedMonthLabel.replaceFirstChar { it.uppercase() })
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir mes")
                }
                DropdownMenu(
                    expanded = comparisonMenuExpanded,
                    onDismissRequest = { onComparisonMenuExpandedChange(false) }
                ) {
                    comparisonMonths.forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(formatMonthHeader(month).replaceFirstChar { it.uppercase() })
                            },
                            onClick = {
                                onComparisonMonthChange(month)
                                onComparisonMenuExpandedChange(false)
                            }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ComparisonTotal(
                    label = "Este mes",
                    amount = comparison.currentMonthTotalCents,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ComparisonTotal(
                    label = selectedMonthLabel.replaceFirstChar { it.uppercase() },
                    amount = comparison.comparisonMonthTotalCents,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
            }
            SpendingLineChart(
                lines = listOf(
                    ChartLine("Este mes", MaterialTheme.colorScheme.primary, List(pointCount) {
                        currentPoints.getOrNull(it)
                    }),
                    ChartLine(selectedMonthLabel, color, List(pointCount) {
                        priorPoints.getOrNull(it)
                    })
                ),
                labels = (1..pointCount).map { "Día $it" },
                currencyCode = currencyCode
            )
        }
    }
}

package com.example.nummia.views.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.calculations.CategoryChartPeriod
import com.example.nummia.calculations.CategoryMonthComparison
import com.example.nummia.calculations.SpendingPoint
import com.example.nummia.calculations.TransactionStatisticsCalculator
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.expenseColor
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.formatMonthHeader

/** Muestra la evolución de una categoría y su comparación acumulada con otro mes. */
@Composable
fun CategoryStatisticsScreen(
    category: String,
    period: CategoryChartPeriod,
    series: List<SpendingPoint>,
    comparison: CategoryMonthComparison,
    comparisonMonth: Long,
    comparisonMonths: List<Long>,
    onPeriodChange: (CategoryChartPeriod) -> Unit,
    onComparisonMonthChange: (Long) -> Unit,
    onBack: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    val selectedMonth = formatMonthHeader(comparisonMonth)
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var showCumulative by remember { mutableStateOf(false) }
    val displayedSeries = TransactionStatisticsCalculator.cumulativeSeries(series, showCumulative)
    val currentComparisonPoints = comparison.currentMonthPoints.map { it.amountCents }
    val priorComparisonPoints = comparison.comparisonMonthPoints.map { it.amountCents }
    val comparisonPointCount = maxOf(currentComparisonPoints.size, priorComparisonPoints.size)
    val comparisonLines = listOf(
        ChartLine(
            "Este mes",
            MaterialTheme.colorScheme.primary,
            List(comparisonPointCount) { currentComparisonPoints.getOrNull(it) }
        ),
        ChartLine(
            selectedMonth.replaceFirstChar { it.uppercase() },
            expenseColor,
            List(comparisonPointCount) { priorComparisonPoints.getOrNull(it) }
        )
    )

    ScreenColumn {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column {
                Text(
                    "DETALLE POR CATEGORÍA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(category, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "PERIODO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChartPeriod.entries.forEach { option ->
                FilterChip(
                    selected = period == option,
                    onClick = { onPeriodChange(option) },
                    label = { Text(option.label, maxLines = 1) }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("GASTO EN ${category.uppercase()}", style = MaterialTheme.typography.labelMedium)
                        Text(period.label, style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Text(
                            if (showCumulative) "ACUMULADO" else "POR PERIODO",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    formatMoney(series.sumOf { it.amountCents }, currencyCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Acumular gastos", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (showCumulative) "Suma progresiva en el tiempo"
                            else "Importe de cada día o mes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = showCumulative, onCheckedChange = { showCumulative = it })
                }
                SpendingLineChart(
                    lines = listOf(
                        ChartLine(
                            category,
                            MaterialTheme.colorScheme.primary,
                            displayedSeries.map { it.amountCents }
                        )
                    ),
                    labels = displayedSeries.map { it.label },
                    currencyCode = currencyCode
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Comparativa mensual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Gasto acumulado día a día",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Comparar con", style = MaterialTheme.typography.labelMedium)
                    }
                    Box {
                        OutlinedButton(onClick = { monthMenuExpanded = true }) {
                            Text(selectedMonth.replaceFirstChar { it.uppercase() })
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Elegir mes")
                        }
                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            comparisonMonths.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(formatMonthHeader(month).replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        onComparisonMonthChange(month)
                                        monthMenuExpanded = false
                                    }
                                )
                            }
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
                        label = selectedMonth.replaceFirstChar { it.uppercase() },
                        amount = comparison.comparisonMonthTotalCents,
                        color = expenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
                SpendingLineChart(
                    lines = comparisonLines,
                    labels = (1..comparisonPointCount).map { "Día $it" },
                    currencyCode = currencyCode
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    comparisonLines.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.padding(end = 6.dp)
                                    .height(10.dp)
                                    .width(10.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(line.color)
                            )
                            Text(line.label, style = MaterialTheme.typography.labelSmall, color = line.color)
                        }
                    }
                }
            }
        }
    }
}

package com.example.nummia.views.statistics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.calculations.CategoryChartPeriod
import com.example.nummia.calculations.SpendingPoint
import com.example.nummia.calculations.TransactionStatisticsCalculator
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.expenseColor
import com.example.nummia.views.components.formatMonthHeader
import com.example.nummia.views.components.incomeColor
import java.util.Calendar

/** Presenta las series de movimientos del periodo y, si se solicita, la comparativa mensual. */
@Composable
fun TransactionStatisticsScreen(
    movements: List<MoneyMovement>,
    type: TransactionType,
    openComparisonOnStart: Boolean,
    combined: Boolean = false,
    onCategoryClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    val calculationNow = remember { System.currentTimeMillis() }
    val isIncome = type == TransactionType.INCOME
    val title = if (combined) "Ingresos y gastos" else if (isIncome) "Ingresos" else "Gastos"
    val color = if (isIncome) incomeColor else expenseColor
    var period by remember(type) { mutableStateOf(CategoryChartPeriod.THIS_MONTH) }
    var cumulative by remember(type) { mutableStateOf(false) }
    var selectedPoint by remember(type) { mutableIntStateOf(-1) }
    val currentMonthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val comparisonMonths = remember(type, movements) {
        TransactionStatisticsCalculator.transactionComparisonMonths(movements, type)
    }
    var comparisonMonth by remember(type, comparisonMonths) {
        mutableLongStateOf(comparisonMonths.firstOrNull() ?: currentMonthStart)
    }
    var comparisonMenuExpanded by remember { mutableStateOf(false) }
    // En la vista conjunta, ambas series parten del mismo movimiento para alinear los puntos.
    val allTimeStart = remember(movements, combined, calculationNow) {
        if (combined) movements.filter { it.timestamp <= calculationNow }.minOfOrNull { it.timestamp }
        else null
    }
    val periodData = remember(movements, type, period, allTimeStart, calculationNow) {
        TransactionStatisticsCalculator.transactionPeriodData(
            movements,
            type,
            period,
            calculationNow,
            allTimeStart
        )
    }
    val incomeData = remember(movements, combined, period, allTimeStart, calculationNow) {
        if (combined) {
            TransactionStatisticsCalculator.transactionPeriodData(
                movements,
                TransactionType.INCOME,
                period,
                calculationNow,
                allTimeStart
            )
        } else null
    }
    val expenseData = remember(movements, combined, period, allTimeStart, calculationNow) {
        if (combined) {
            TransactionStatisticsCalculator.transactionPeriodData(
                movements,
                TransactionType.EXPENSE,
                period,
                calculationNow,
                allTimeStart
            )
        } else null
    }
    val chartData = expenseData ?: periodData
    val chartSeries = chartData.points
    val displayedSeries = remember(chartSeries, cumulative) {
        TransactionStatisticsCalculator.cumulativeSeries(chartSeries, cumulative)
    }
    val displayedIncome = remember(incomeData, cumulative) {
        incomeData?.let { TransactionStatisticsCalculator.cumulativeSeries(it.points, cumulative) }.orEmpty()
    }
    val displayedExpenses = remember(expenseData, cumulative) {
        expenseData?.let { TransactionStatisticsCalculator.cumulativeSeries(it.points, cumulative) }.orEmpty()
    }
    val breakdown = remember(chartData, selectedPoint, cumulative, combined, type) {
        TransactionStatisticsCalculator.categoryBreakdown(
            periodData = chartData,
            selectedPointIndex = selectedPoint.takeIf { it >= 0 },
            cumulative = cumulative
        )
    }
    val shownTotal = breakdown.sumOf { it.amountCents }
    fun displayedAmount(points: List<SpendingPoint>): Long =
        selectedPoint.takeIf { it >= 0 }
            ?.let { points.getOrNull(it)?.amountCents }
            ?: (if (cumulative) points.lastOrNull()?.amountCents ?: 0L else points.sumOf { it.amountCents })
    val shownIncome = displayedAmount(displayedIncome)
    val shownExpenses = displayedAmount(displayedExpenses)
    val chartLines = if (combined) {
        listOf(
            ChartLine("Ingresos", incomeColor, displayedIncome.map { it.amountCents }),
            ChartLine("Gastos", expenseColor, displayedExpenses.map { it.amountCents })
        )
    } else {
        listOf(ChartLine(title, color, displayedSeries.map { it.amountCents }))
    }
    val comparison = remember(movements, type, comparisonMonth, calculationNow) {
        TransactionStatisticsCalculator.transactionMonthComparison(
            movements = movements,
            type = type,
            comparisonMonthStart = comparisonMonth,
            now = calculationNow
        )
    }
    val selectedMonthLabel = formatMonthHeader(comparisonMonth)

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Column {
                Text(
                    "DETALLE DE MOVIMIENTOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                    onClick = {
                        period = option
                        selectedPoint = -1
                    },
                    label = { Text(option.label, maxLines = 1) }
                )
            }
        }

        TransactionPeriodSummaryCard(
            title = title,
            period = period,
            cumulative = cumulative,
            combined = combined,
            shownIncome = shownIncome,
            shownExpenses = shownExpenses,
            shownTotal = shownTotal,
            currencyCode = currencyCode,
            chartLines = chartLines,
            chartLabels = (if (combined) displayedExpenses else displayedSeries).map { it.label },
            chartSeries = chartSeries,
            selectedPoint = selectedPoint,
            breakdown = breakdown,
            onCumulativeChange = {
                cumulative = it
                selectedPoint = -1
            },
            onSelectionChange = { selectedPoint = it },
            onCategoryClick = onCategoryClick
        )

        if (openComparisonOnStart) {
            TransactionMonthComparisonCard(
                comparisonMonths = comparisonMonths,
                selectedMonthLabel = selectedMonthLabel,
                comparison = comparison,
                comparisonMenuExpanded = comparisonMenuExpanded,
                color = color,
                currencyCode = currencyCode,
                onComparisonMonthChange = { comparisonMonth = it },
                onComparisonMenuExpandedChange = { comparisonMenuExpanded = it }
            )
        }
    }
}

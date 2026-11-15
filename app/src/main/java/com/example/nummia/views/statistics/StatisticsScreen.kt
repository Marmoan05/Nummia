package com.example.nummia.views.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.calculations.StatisticsSummary
import com.example.nummia.model.transactions.TransactionType
import com.example.nummia.views.components.ComparisonBar
import com.example.nummia.views.components.EmptyMessage
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import com.example.nummia.views.components.SectionTitle
import com.example.nummia.views.components.SummaryAmountCard
import com.example.nummia.views.components.expenseColor
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.incomeColor

/** Muestra totales mensuales, comparación con el mes anterior y gastos por categoría. */
@Composable
fun StatisticsScreen(
    summary: StatisticsSummary,
    onOpenSettings: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onOpenStatistics: (TransactionType) -> Unit,
    onOpenComparison: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    val income = summary.incomeThisMonthCents
    val spent = summary.expensesThisMonthCents
    val previousMonthSpent = summary.expensesPreviousMonthCents
    val categoryTotals = summary.expensesByCategoryCents
    val difference = summary.expenseDifferenceCents
    val largestCategory = categoryTotals.values.maxOrNull() ?: 1L

    ScreenColumn {
        ScreenHeading("Estadísticas", "Entiende cómo se mueve tu dinero") {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryAmountCard(
                "Ingresos", income, true, Modifier.weight(1f),
                onClick = { onOpenStatistics(TransactionType.INCOME) }
            )
            SummaryAmountCard(
                "Gastos", spent, false, Modifier.weight(1f),
                onClick = { onOpenStatistics(TransactionType.EXPENSE) }
            )
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenComparison)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Comparación mensual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Este mes: ${formatMoney(spent, currencyCode)}")
                Text("Mes anterior: ${formatMoney(previousMonthSpent, currencyCode)}")
                Text(
                    if (difference > 0) "Has gastado ${formatMoney(difference, currencyCode)} más que el mes anterior"
                    else "Has gastado ${formatMoney(-difference, currencyCode)} menos que el mes anterior",
                    color = if (difference > 0) expenseColor else incomeColor,
                    fontWeight = FontWeight.Medium
                )
                ComparisonBar("Mes anterior", previousMonthSpent, maxOf(spent, previousMonthSpent))
                ComparisonBar("Este mes", spent, maxOf(spent, previousMonthSpent))
            }
        }
        SectionTitle("Gastos por categoría")
        if (categoryTotals.isEmpty()) EmptyMessage("Registra gastos para ver el desglose.")
        categoryTotals.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
            Column(
                Modifier.fillMaxWidth()
                    .clickable { onCategoryClick(category) }
                    .padding(vertical = 7.dp)
            ) {
                Row {
                    Text(category)
                    Spacer(Modifier.weight(1f))
                    Text(formatMoney(amount, currencyCode), fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (amount.toFloat() / largestCategory).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

package com.example.nummia.calculations

import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

/** Calcula el desglose porcentual por categoría de los intervalos seleccionados. */
internal object StatisticsBreakdownCalculator {
    fun cumulativeSeries(
        points: List<SpendingPoint>,
        cumulative: Boolean
    ): List<SpendingPoint> {
        if (!cumulative) return points
        var total = 0L
        return points.map { point ->
            total += point.amountCents
            point.copy(amountCents = total)
        }
    }

    fun categoryBreakdown(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        selectedPointIndex: Int?,
        cumulative: Boolean,
        now: Long,
        rangeStartOverride: Long?
    ): List<StatisticsCategoryTotal> {
        val periodData = StatisticsPeriodCalculator.transactionPeriodData(
            movements,
            type,
            period,
            now,
            rangeStartOverride
        )
        return categoryBreakdown(periodData, selectedPointIndex, cumulative)
    }

    fun categoryBreakdown(
        periodData: TransactionPeriodData,
        selectedPointIndex: Int?,
        cumulative: Boolean
    ): List<StatisticsCategoryTotal> {
        if (periodData.points.isEmpty()) return emptyList()
        val endIndex = selectedPointIndex?.coerceIn(periodData.points.indices)
            ?: periodData.points.lastIndex
        val startIndex = if (cumulative) {
            0
        } else {
            selectedPointIndex?.coerceIn(periodData.points.indices) ?: 0
        }
        val totals = mutableMapOf<String, Long>()
        for (index in startIndex..endIndex) {
            periodData.categoryAmountsByPoint[index].forEach { (category, amount) ->
                totals[category] = (totals[category] ?: 0L) + amount
            }
        }
        val nonZeroTotals = totals.filterValues { it > 0L }
        val total = nonZeroTotals.values.sum()
        return nonZeroTotals.entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                StatisticsCategoryTotal(
                    category = category,
                    amountCents = amount,
                    percentage = if (total > 0L) amount.toFloat() / total else 0f
                )
            }
    }
}

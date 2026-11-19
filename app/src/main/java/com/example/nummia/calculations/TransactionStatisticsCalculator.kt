package com.example.nummia.calculations

import com.example.nummia.model.state.AppState
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

/** Expone la API de estadísticas de movimientos delegando a calculadores especializados. */
object TransactionStatisticsCalculator {
    fun cumulativeSeries(
        points: List<SpendingPoint>,
        cumulative: Boolean
    ): List<SpendingPoint> =
        StatisticsBreakdownCalculator.cumulativeSeries(points, cumulative)

    fun calculate(
        state: AppState,
        now: Long = System.currentTimeMillis()
    ): StatisticsSummary = StatisticsSummaryCalculator.calculate(state, now)

    fun transactionSeries(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        now: Long = System.currentTimeMillis(),
        rangeStartOverride: Long? = null
    ): List<SpendingPoint> = StatisticsPeriodCalculator.transactionSeries(
        movements,
        type,
        period,
        now,
        rangeStartOverride
    )

    fun transactionPeriodData(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        now: Long = System.currentTimeMillis(),
        rangeStartOverride: Long? = null
    ): TransactionPeriodData = StatisticsPeriodCalculator.transactionPeriodData(
        movements,
        type,
        period,
        now,
        rangeStartOverride
    )

    fun categoryBreakdown(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        selectedPointIndex: Int?,
        cumulative: Boolean,
        now: Long = System.currentTimeMillis(),
        rangeStartOverride: Long? = null
    ): List<StatisticsCategoryTotal> = StatisticsBreakdownCalculator.categoryBreakdown(
        movements,
        type,
        period,
        selectedPointIndex,
        cumulative,
        now,
        rangeStartOverride
    )

    fun categoryBreakdown(
        periodData: TransactionPeriodData,
        selectedPointIndex: Int?,
        cumulative: Boolean
    ): List<StatisticsCategoryTotal> =
        StatisticsBreakdownCalculator.categoryBreakdown(
            periodData,
            selectedPointIndex,
            cumulative
        )

    fun transactionMonthComparison(
        movements: List<MoneyMovement>,
        type: TransactionType,
        comparisonMonthStart: Long,
        now: Long = System.currentTimeMillis()
    ): CategoryMonthComparison =
        StatisticsMonthComparisonCalculator.transactionMonthComparison(
            movements,
            type,
            comparisonMonthStart,
            now
        )

    fun transactionComparisonMonths(
        movements: List<MoneyMovement>,
        type: TransactionType,
        now: Long = System.currentTimeMillis()
    ): List<Long> =
        StatisticsMonthComparisonCalculator.transactionComparisonMonths(
            movements,
            type,
            now
        )
}

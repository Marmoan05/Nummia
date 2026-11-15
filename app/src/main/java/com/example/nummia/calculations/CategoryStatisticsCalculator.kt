package com.example.nummia.calculations

import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

/** Calcula series, comparativas y meses disponibles para el detalle de una categoría. */
object CategoryStatisticsCalculator {
    /** Agrupa los gastos de una categoría por día o mes para dibujar su evolución. */
    fun spendingSeries(
        movements: List<MoneyMovement>,
        category: String,
        period: CategoryChartPeriod,
        now: Long = System.currentTimeMillis()
    ): List<SpendingPoint> = TransactionStatisticsCalculator.transactionSeries(
        movements.filter { it.category == category },
        TransactionType.EXPENSE,
        period,
        now
    )

    /** Compara el gasto acumulado diario de una categoría con otro mes natural. */
    fun monthComparison(
        movements: List<MoneyMovement>,
        category: String,
        comparisonMonthStart: Long,
        now: Long = System.currentTimeMillis()
    ): CategoryMonthComparison = TransactionStatisticsCalculator.transactionMonthComparison(
        movements.filter { it.category == category },
        TransactionType.EXPENSE,
        comparisonMonthStart,
        now
    )

    /** Devuelve los meses anteriores con gastos de esta categoría, del más reciente al antiguo. */
    fun comparisonMonths(
        movements: List<MoneyMovement>,
        category: String,
        now: Long = System.currentTimeMillis()
    ): List<Long> = TransactionStatisticsCalculator.transactionComparisonMonths(
        movements.filter { it.category == category },
        TransactionType.EXPENSE,
        now
    )
}

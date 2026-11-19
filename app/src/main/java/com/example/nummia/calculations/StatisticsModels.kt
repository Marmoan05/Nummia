package com.example.nummia.calculations

/** Intervalos que pueden seleccionarse en las gráficas de movimientos y categorías. */
enum class CategoryChartPeriod(val label: String) {
    LAST_7_DAYS("Últimos 7 días"),
    THIS_MONTH("Este mes"),
    LAST_3_MONTHS("Últimos 3 meses"),
    LAST_6_MONTHS("Últimos 6 meses"),
    LAST_YEAR("Último año"),
    ALL_TIME("Total")
}

/** Importe en céntimos asociado a un intervalo y a su etiqueta visible. */
data class SpendingPoint(val timestamp: Long, val label: String, val amountCents: Long)

/** Series diarias del mes actual y del mes comparado, con sus totales acumulados. */
data class CategoryMonthComparison(
    val currentMonthPoints: List<SpendingPoint>,
    val comparisonMonthPoints: List<SpendingPoint>,
    val currentMonthTotalCents: Long,
    val comparisonMonthTotalCents: Long
)

/** Total de una categoría; percentage es una fracción entre 0 y 1. */
data class StatisticsCategoryTotal(
    val category: String,
    val amountCents: Long,
    val percentage: Float
)

/** Serie total y desglose por categoría alineados por índice de punto. */
data class TransactionPeriodData(
    val points: List<SpendingPoint>,
    val categoryAmountsByPoint: List<Map<String, Long>>
)

/** Resultado agregado usado por la pantalla de estadísticas. */
data class StatisticsSummary(
    val incomeThisMonthCents: Long,
    val expensesThisMonthCents: Long,
    val expensesPreviousMonthCents: Long,
    val expensesByCategoryCents: Map<String, Long>
) {
    /** Diferencia entre gastos del mes actual y los del mes anterior. */
    val expenseDifferenceCents: Long
        get() = expensesThisMonthCents - expensesPreviousMonthCents
}

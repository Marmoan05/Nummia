package com.example.nummia.controller

import com.example.nummia.calculations.TransactionStatisticsCalculator
import com.example.nummia.calculations.StatisticsSummary
import com.example.nummia.model.state.AppState

/** Proporciona a la interfaz el resumen de ingresos y gastos calculado. */
class StatisticsController {
    /** Calcula estadísticas mensuales y distribución de gastos por categoría. */
    fun calculate(state: AppState): StatisticsSummary = TransactionStatisticsCalculator.calculate(state)
}

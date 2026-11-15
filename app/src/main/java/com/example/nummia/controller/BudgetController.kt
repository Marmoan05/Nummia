package com.example.nummia.controller

import com.example.nummia.calculations.BudgetCalculator
import com.example.nummia.calculations.BudgetSummary
import com.example.nummia.model.state.AppState
import com.example.nummia.model.FinanceModel

/** Atiende cambios de presupuesto y calcula el gasto de los periodos actuales. */
class BudgetController(private val model: FinanceModel) {
    /** Guarda los límites de gasto semanal y mensual solicitados por la interfaz. */
    fun setBudgets(weeklyCents: Long, monthlyCents: Long) =
        model.setBudgets(weeklyCents, monthlyCents)

    /** Obtiene el resumen de consumo semanal y mensual. */
    fun calculate(state: AppState): BudgetSummary = BudgetCalculator.calculate(state)
}

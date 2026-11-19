package com.example.nummia.model.state

/** Crea el estado vacío que se completa durante la configuración inicial. */
internal fun createInitialFinanceState(): AppState = AppState(
    initialBalanceCents = 0L,
    movements = emptyList(),
    plannedExpenses = emptyList(),
    weeklyBudgetCents = 0L,
    monthlyBudgetCents = 0L
)

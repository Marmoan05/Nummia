package com.example.nummia.controller

import com.example.nummia.calculations.BalanceCalculator
import com.example.nummia.model.state.AppState
import com.example.nummia.model.FinanceModel

/** Atiende las acciones de la interfaz relacionadas con el saldo. */
class BalanceController(private val model: FinanceModel) {
    /** Envía al modelo el nuevo saldo inicial introducido por el usuario. */
    fun setInitialBalance(amountCents: Long) = model.setInitialBalance(amountCents)

    /** Calcula el saldo disponible a partir del estado financiero. */
    fun calculate(state: AppState): Long = BalanceCalculator.calculate(state)
}

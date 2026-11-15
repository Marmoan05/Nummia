package com.example.nummia.calculations

import com.example.nummia.model.state.AppState
import com.example.nummia.model.transactions.TransactionType

/** Contiene la operación de cálculo del saldo, independiente de la interfaz y la base de datos. */
object BalanceCalculator {
    /** Suma ingresos y resta gastos al saldo inicial; los importes están expresados en céntimos. */
    fun calculate(state: AppState): Long =
        state.initialBalanceCents + state.movements.sumOf {
            if (it.type == TransactionType.INCOME) it.amountCents else -it.amountCents
        }
}

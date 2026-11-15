package com.example.nummia.controller

import com.example.nummia.model.FinanceModel
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense

/** Gestiona las acciones de la interfaz sobre pagos previstos. */
class PlannedExpenseController(private val model: FinanceModel) {
    /** Envía al modelo los datos de facturación y su repetición. */
    fun addPlannedExpense(
        title: String,
        description: String?,
        amountCents: Long,
        category: String,
        dueDate: String,
        dueAt: Long,
        frequency: BillingFrequency,
        billingDay: Int,
        repeatUntilAt: Long?,
        includeUntilPeriod: Boolean
    ) = model.addPlannedExpense(
        title,
        description,
        amountCents,
        category,
        dueDate,
        dueAt,
        frequency,
        billingDay,
        repeatUntilAt,
        includeUntilPeriod
    )

    /** Actualiza una factura pendiente, incluida su descripción y repetición. */
    fun updatePlannedExpense(expense: PlannedExpense) = model.updatePlannedExpense(expense)

    /** Elimina una factura pendiente. */
    fun deletePlannedExpense(expenseId: Long) = model.deletePlannedExpense(expenseId)

    /** Posponer o desactivar avisos locales de una factura. */
    fun snoozeReminder(expenseId: Long) = model.snoozePlannedExpenseReminder(expenseId)
    fun disableReminders(expenseId: Long) = model.disablePlannedExpenseReminders(expenseId)

    /** Registra como gasto el pago confirmado, incluyendo el extra opcional por retraso. */
    fun markPlannedExpensePaid(expenseId: Long, extraCents: Long) =
        model.markPlannedExpensePaid(expenseId, extraCents)
}

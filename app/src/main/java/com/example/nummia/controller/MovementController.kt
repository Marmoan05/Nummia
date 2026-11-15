package com.example.nummia.controller

import com.example.nummia.model.FinanceModel
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

/** Recibe de la interfaz las acciones sobre ingresos y gastos. */
class MovementController(private val model: FinanceModel) {
    /** Envía un movimiento validado por la interfaz al modelo financiero. */
    fun addMovement(
        title: String,
        description: String?,
        amountCents: Long,
        category: String,
        type: TransactionType,
        timestamp: Long
    ) = model.addMovement(title, description, amountCents, category, type, timestamp)

    /** Actualiza un movimiento existente. */
    fun updateMovement(movement: MoneyMovement) = model.updateMovement(movement)

    /** Elimina un movimiento por su identificador. */
    fun deleteMovement(movementId: Long) = model.deleteMovement(movementId)
}

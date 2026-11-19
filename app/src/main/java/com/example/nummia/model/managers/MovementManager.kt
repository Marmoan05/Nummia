package com.example.nummia.model.managers

import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.FinanceStateUpdater
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

/** Gestiona los movimientos y mantiene el historial sincronizado con Room. */
internal class MovementManager(
    private val repository: FinanceRepository,
    private val updateState: FinanceStateUpdater
) {
    /** Crea y guarda un ingreso o gasto, y lo añade al estado visible. */
    fun addMovement(
        title: String,
        description: String?,
        amountCents: Long,
        category: String,
        type: TransactionType,
        timestamp: Long
    ) {
        updateState { state ->
            val movement = MoneyMovement(
                title = title,
                description = description,
                amountCents = amountCents,
                category = category,
                type = type,
                timestamp = timestamp
            )
            val savedMovement = movement.copy(id = repository.addMovement(movement))
            state.copy(
                movements = (listOf(savedMovement) + state.movements)
                    .sortedByDescending { it.timestamp }
            )
        }
    }

    /** Actualiza un movimiento y conserva el orden cronológico del historial. */
    fun updateMovement(movement: MoneyMovement) {
        updateState { state ->
            repository.updateMovement(movement)
            state.copy(
                movements = state.movements
                    .map { if (it.id == movement.id) movement else it }
                    .sortedByDescending { it.timestamp }
            )
        }
    }

    /** Elimina un movimiento del almacenamiento y del estado visible. */
    fun deleteMovement(movementId: Long) {
        updateState { state ->
            repository.deleteMovement(movementId)
            state.copy(movements = state.movements.filterNot { it.id == movementId })
        }
    }
}

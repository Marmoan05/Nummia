package com.example.nummia.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nummia.model.settings.FinanceSettings
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.plannedexpenses.PlannedExpense

/** Define las consultas Room para leer y guardar preferencias, movimientos y pagos. */
@Dao
interface FinanceDao {
    /** Recupera los límites y el saldo inicial guardados, o null en la primera ejecución. */
    @Query("SELECT * FROM account_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): FinanceSettings?

    /** Inserta o reemplaza la configuración única de la cuenta. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: FinanceSettings)

    /** Lee el historial ordenado del movimiento más reciente al más antiguo. */
    @Query("SELECT * FROM movements ORDER BY timestamp DESC")
    suspend fun getMovements(): List<MoneyMovement>

    /** Inserta un movimiento y devuelve el identificador generado por Room. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: MoneyMovement): Long

    /** Actualiza los campos de un movimiento existente. */
    @Update
    suspend fun updateMovement(movement: MoneyMovement)

    /** Actualiza importes de varios movimientos durante un cambio de moneda. */
    @Update
    suspend fun updateMovements(movements: List<MoneyMovement>)

    /** Elimina un movimiento existente. */
    @Query("DELETE FROM movements WHERE id = :movementId")
    suspend fun deleteMovement(movementId: Long)

    /** Lee todos los pagos previstos ordenados por identificador descendente. */
    @Query("SELECT * FROM planned_expenses ORDER BY id DESC")
    suspend fun getPlannedExpenses(): List<PlannedExpense>

    /** Inserta un pago previsto y devuelve el identificador generado por Room. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlannedExpense(expense: PlannedExpense): Long

    /** Actualiza importes de varios pagos previstos durante un cambio de moneda. */
    @Update
    suspend fun updatePlannedExpenses(expenses: List<PlannedExpense>)

    /** Actualiza los datos de una factura o su siguiente fecha tras el pago. */
    @Update
    suspend fun updatePlannedExpense(expense: PlannedExpense)

    /** Elimina una factura de pago único después de registrarla como gasto. */
    @Query("DELETE FROM planned_expenses WHERE id = :expenseId")
    suspend fun deletePlannedExpense(expenseId: Long)

    /** Lee las facturas para reprogramar sus recordatorios. */
    @Query("SELECT * FROM planned_expenses")
    suspend fun getAllPlannedExpenses(): List<PlannedExpense>
}

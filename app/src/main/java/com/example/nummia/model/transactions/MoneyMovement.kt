package com.example.nummia.model.transactions
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Entidad Room que representa un ingreso o gasto del historial. */
@Entity(tableName = "movements")
data class MoneyMovement(
    @PrimaryKey
    val id: Long,
    val title: String,
    val amountCents: Long,
    val category: String,
    val type: TransactionType,
    val timestamp: Long,
    val description: String? = null
)

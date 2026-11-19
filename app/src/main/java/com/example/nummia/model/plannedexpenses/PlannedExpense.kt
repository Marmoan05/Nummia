package com.example.nummia.model.plannedexpenses
import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

/** Entidad Room que representa un pago anotado para realizarse en el futuro. */
@Entity(tableName = "planned_expenses")
data class PlannedExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val amountCents: Long,
    val dueDate: String,
    val category: String,
    @ColumnInfo(defaultValue = "'NONE'")
    val frequency: BillingFrequency = BillingFrequency.NONE,
    @ColumnInfo(defaultValue = "1")
    val billingDay: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val nextDueAt: Long = 0L,
    val repeatUntilAt: Long? = null,
    @ColumnInfo(defaultValue = "1")
    val includeUntilPeriod: Boolean = true,
    @ColumnInfo(defaultValue = "NULL")
    val description: String? = null,
    @ColumnInfo(defaultValue = "0")
    val remindersDisabled: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val reminderSnoozedUntil: Long = 0L
)

/** Repetición configurada para una factura pendiente. */
enum class BillingFrequency {
    NONE,
    WEEKLY,
    MONTHLY
}

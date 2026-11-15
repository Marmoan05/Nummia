package com.example.nummia.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.nummia.model.settings.FinanceSettings
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.transactions.TransactionType
import android.content.Context
import androidx.room.Room

/** Configura la base Room y expone el DAO de datos financieros. */
@Database(
    entities = [FinanceSettings::class, MoneyMovement::class, PlannedExpense::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(FinanceTypeConverters::class)
abstract class FinanceDatabase : RoomDatabase() {
    /** Proporciona el objeto de acceso a los datos que genera Room. */
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var instance: FinanceDatabase? = null

        /** Devuelve la instancia de Room compartida durante la vida del proceso. */
        fun open(context: Context): FinanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance.db"
                )
                    .build()
                    .also { instance = it }
            }
    }
}

/** Convierte enums de Kotlin a texto para almacenarlos en las columnas SQLite de Room. */
class FinanceTypeConverters {
    /** Guarda el tipo del movimiento como el nombre del enum. */
    @TypeConverter
    fun transactionTypeToString(type: TransactionType): String = type.name

    /** Reconstruye el enum de movimiento desde el texto guardado. */
    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    /** Guarda la frecuencia de facturación como texto. */
    @TypeConverter
    fun billingFrequencyToString(frequency: BillingFrequency): String = frequency.name

    /** Reconstruye la frecuencia de facturación desde el valor guardado. */
    @TypeConverter
    fun stringToBillingFrequency(value: String): BillingFrequency = BillingFrequency.valueOf(value)
}

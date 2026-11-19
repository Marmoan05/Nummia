package com.example.nummia.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.settings.FinanceSettings
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

/** Configura la base Room y expone el DAO de datos financieros. */
@Database(
    entities = [FinanceSettings::class, MoneyMovement::class, PlannedExpense::class],
    version = 2,
    exportSchema = true
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE account_settings ADD COLUMN auto_update_checks_enabled " +
                        "INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE account_settings ADD COLUMN onboarding_completed " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL("UPDATE account_settings SET onboarding_completed = 1")

                db.execSQL("ALTER TABLE movements RENAME TO movements_old")
                db.execSQL(
                    "CREATE TABLE movements (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "amountCents INTEGER NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "description TEXT)"
                )
                db.execSQL(
                    "INSERT INTO movements " +
                        "(id, title, amountCents, category, type, timestamp, description) " +
                        "SELECT id, title, amountCents, category, type, timestamp, description " +
                        "FROM movements_old"
                )
                db.execSQL("DROP TABLE movements_old")

                db.execSQL("ALTER TABLE planned_expenses RENAME TO planned_expenses_old")
                db.execSQL(
                    "CREATE TABLE planned_expenses (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "amountCents INTEGER NOT NULL, " +
                        "dueDate TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "frequency TEXT NOT NULL DEFAULT 'NONE', " +
                        "billingDay INTEGER NOT NULL DEFAULT 1, " +
                        "nextDueAt INTEGER NOT NULL DEFAULT 0, " +
                        "repeatUntilAt INTEGER, " +
                        "includeUntilPeriod INTEGER NOT NULL DEFAULT 1, " +
                        "description TEXT DEFAULT NULL, " +
                        "remindersDisabled INTEGER NOT NULL DEFAULT 0, " +
                        "reminderSnoozedUntil INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "INSERT INTO planned_expenses (" +
                        "id, title, amountCents, dueDate, category, frequency, billingDay, " +
                        "nextDueAt, repeatUntilAt, includeUntilPeriod, description, " +
                        "remindersDisabled, reminderSnoozedUntil) " +
                        "SELECT id, title, amountCents, dueDate, category, frequency, " +
                        "billingDay, nextDueAt, repeatUntilAt, includeUntilPeriod, description, " +
                        "remindersDisabled, reminderSnoozedUntil FROM planned_expenses_old"
                )
                db.execSQL("DROP TABLE planned_expenses_old")
            }
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

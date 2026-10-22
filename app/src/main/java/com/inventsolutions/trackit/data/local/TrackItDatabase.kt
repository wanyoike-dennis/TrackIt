package com.inventsolutions.trackit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MoneyTransaction::class, MonthlyBudget::class, ExpenseCategory::class, FinancialRecord::class],
    version = 5,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun financialRecordDao(): FinancialRecordDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS monthly_budgets (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "monthKey TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "limitMinor INTEGER NOT NULL, " +
                        "createdAtEpochMillis INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_budgets_monthKey_category " +
                        "ON monthly_budgets(monthKey, category)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE monthly_budgets ADD COLUMN budgetType TEXT NOT NULL DEFAULT 'ONE_TIME'"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS expense_categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "normalizedName TEXT NOT NULL, " +
                        "createdAtEpochMillis INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_expense_categories_normalizedName " +
                        "ON expense_categories(normalizedName)"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO expense_categories (name, normalizedName, createdAtEpochMillis) " +
                        "SELECT category, lower(trim(category)), CAST(strftime('%s','now') AS INTEGER) * 1000 " +
                        "FROM transactions WHERE type = 'EXPENSE' AND trim(category) != '' GROUP BY lower(trim(category))"
                )
                database.execSQL(
                    "INSERT OR IGNORE INTO expense_categories (name, normalizedName, createdAtEpochMillis) " +
                        "SELECT category, lower(trim(category)), CAST(strftime('%s','now') AS INTEGER) * 1000 " +
                        "FROM monthly_budgets WHERE trim(category) != '' GROUP BY lower(trim(category))"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS financial_records (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "type TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "targetMinor INTEGER NOT NULL, " +
                        "currentMinor INTEGER NOT NULL, " +
                        "dueDateEpochMillis INTEGER, " +
                        "notes TEXT NOT NULL, " +
                        "createdAtEpochMillis INTEGER NOT NULL)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_records_type ON financial_records(type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_records_createdAtEpochMillis ON financial_records(createdAtEpochMillis)")
            }
        }

        @Volatile
        private var INSTANCE: TrackItDatabase? = null

        fun getInstance(context: Context): TrackItDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackItDatabase::class.java,
                    "trackit_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

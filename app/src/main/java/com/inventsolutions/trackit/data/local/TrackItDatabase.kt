package com.inventsolutions.trackit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MoneyTransaction::class, MonthlyBudget::class, ExpenseCategory::class, FinancialRecord::class, FinancialRecordEntry::class, RecurringTransaction::class],
    version = 11,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun financialRecordDao(): FinancialRecordDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS monthly_budgets (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, monthKey TEXT NOT NULL, category TEXT NOT NULL, limitMinor INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_monthly_budgets_monthKey_category ON monthly_budgets(monthKey, category)")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE monthly_budgets ADD COLUMN budgetType TEXT NOT NULL DEFAULT 'ONE_TIME'")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS expense_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, normalizedName TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expense_categories_normalizedName ON expense_categories(normalizedName)")
                database.execSQL("INSERT OR IGNORE INTO expense_categories (name, normalizedName, createdAtEpochMillis) SELECT category, lower(trim(category)), CAST(strftime('%s','now') AS INTEGER) * 1000 FROM transactions WHERE type = 'EXPENSE' AND trim(category) != '' GROUP BY lower(trim(category))")
                database.execSQL("INSERT OR IGNORE INTO expense_categories (name, normalizedName, createdAtEpochMillis) SELECT category, lower(trim(category)), CAST(strftime('%s','now') AS INTEGER) * 1000 FROM monthly_budgets WHERE trim(category) != '' GROUP BY lower(trim(category))")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS financial_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, title TEXT NOT NULL, targetMinor INTEGER NOT NULL, currentMinor INTEGER NOT NULL, dueDateEpochMillis INTEGER, notes TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_records_type ON financial_records(type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_records_createdAtEpochMillis ON financial_records(createdAtEpochMillis)")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS financial_record_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, financialRecordId INTEGER NOT NULL, entryType TEXT NOT NULL, amountMinor INTEGER NOT NULL, note TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(financialRecordId) REFERENCES financial_records(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_record_entries_financialRecordId ON financial_record_entries(financialRecordId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_record_entries_createdAtEpochMillis ON financial_record_entries(createdAtEpochMillis)")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE financial_records ADD COLUMN savingsCategory TEXT NOT NULL DEFAULT 'General savings'")
                database.execSQL("ALTER TABLE financial_records ADD COLUMN contributionMethod TEXT NOT NULL DEFAULT 'FLEXIBLE'")
                database.execSQL("ALTER TABLE financial_records ADD COLUMN plannedContributionMinor INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN allowanceRecordId INTEGER")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS recurring_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, type TEXT NOT NULL, amountMinor INTEGER NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, paymentMethod TEXT NOT NULL, frequency TEXT NOT NULL, nextRunEpochMillis INTEGER NOT NULL, active INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_nextRunEpochMillis ON recurring_transactions(nextRunEpochMillis)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_active ON recurring_transactions(active)")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS hustles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, normalizedName TEXT NOT NULL, description TEXT NOT NULL, active INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_hustles_normalizedName ON hustles(normalizedName)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustles_active ON hustles(active)")
                database.execSQL("CREATE TABLE IF NOT EXISTS hustle_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hustleId INTEGER NOT NULL, type TEXT NOT NULL, amountMinor INTEGER NOT NULL, category TEXT NOT NULL, description TEXT NOT NULL, paymentMethod TEXT NOT NULL, dateEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(hustleId) REFERENCES hustles(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_transactions_hustleId ON hustle_transactions(hustleId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_transactions_type ON hustle_transactions(type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_transactions_dateEpochMillis ON hustle_transactions(dateEpochMillis)")
                database.execSQL("CREATE TABLE IF NOT EXISTS hustle_debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hustleId INTEGER NOT NULL, direction TEXT NOT NULL, counterparty TEXT NOT NULL, description TEXT NOT NULL, originalAmountMinor INTEGER NOT NULL, dueDateEpochMillis INTEGER, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(hustleId) REFERENCES hustles(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_debts_hustleId ON hustle_debts(hustleId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_debts_direction ON hustle_debts(direction)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_debts_dueDateEpochMillis ON hustle_debts(dueDateEpochMillis)")
                database.execSQL("CREATE TABLE IF NOT EXISTS hustle_debt_settlements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, debtId INTEGER NOT NULL, amountMinor INTEGER NOT NULL, note TEXT NOT NULL, dateEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, FOREIGN KEY(debtId) REFERENCES hustle_debts(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_debt_settlements_debtId ON hustle_debt_settlements(debtId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_hustle_debt_settlements_dateEpochMillis ON hustle_debt_settlements(dateEpochMillis)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS hustle_debt_settlements")
                database.execSQL("DROP TABLE IF EXISTS hustle_transactions")
                database.execSQL("DROP TABLE IF EXISTS hustle_debts")
                database.execSQL("DROP TABLE IF EXISTS hustles")
            }
        }

        @Volatile private var INSTANCE: TrackItDatabase? = null

        fun getInstance(context: Context): TrackItDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, TrackItDatabase::class.java, "trackit_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

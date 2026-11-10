package com.inventsolutions.trackit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MoneyTransaction::class, MonthlyBudget::class, ExpenseCategory::class, FinancialRecord::class, FinancialRecordEntry::class, RecurringTransaction::class, FinancialAccount::class, AccountTransfer::class, AccountReconciliation::class, PendingMpesaTransaction::class],
    version = 15,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao
    abstract fun financialRecordDao(): FinancialRecordDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun financialAccountDao(): FinancialAccountDao
    abstract fun accountTransferDao(): AccountTransferDao
    abstract fun accountReconciliationDao(): AccountReconciliationDao
    abstract fun pendingMpesaTransactionDao(): PendingMpesaTransactionDao

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
        // Keep the version step for installations upgrading from database version 9.
        // Hustles tables are no longer created by this migration.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) = Unit
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS hustle_debt_settlements")
                database.execSQL("DROP TABLE IF EXISTS hustle_transactions")
                database.execSQL("DROP TABLE IF EXISTS hustle_debts")
                database.execSQL("DROP TABLE IF EXISTS hustles")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS financial_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, openingBalanceMinor INTEGER NOT NULL, openingDateEpochMillis INTEGER NOT NULL, currency TEXT NOT NULL, archived INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_accounts_archived ON financial_accounts(archived)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_accounts_createdAtEpochMillis ON financial_accounts(createdAtEpochMillis)")
                database.execSQL("CREATE TABLE IF NOT EXISTS account_transfers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fromAccountId INTEGER NOT NULL, toAccountId INTEGER NOT NULL, amountMinor INTEGER NOT NULL, note TEXT NOT NULL, dateEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_account_transfers_fromAccountId ON account_transfers(fromAccountId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_account_transfers_toAccountId ON account_transfers(toAccountId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_account_transfers_dateEpochMillis ON account_transfers(dateEpochMillis)")
                database.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER")
                database.execSQL("ALTER TABLE recurring_transactions ADD COLUMN accountId INTEGER")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE financial_record_entries ADD COLUMN accountId INTEGER")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_record_entries_accountId ON financial_record_entries(accountId)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS account_reconciliations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, accountId INTEGER NOT NULL, statementBalanceMinor INTEGER NOT NULL, systemBalanceMinor INTEGER NOT NULL, adjustmentMinor INTEGER NOT NULL, note TEXT NOT NULL, reconciledAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_account_reconciliations_accountId ON account_reconciliations(accountId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_account_reconciliations_reconciledAtEpochMillis ON account_reconciliations(reconciledAtEpochMillis)")
            }
        }


        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS pending_mpesa_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, reference TEXT NOT NULL, type TEXT NOT NULL, amountMinor INTEGER NOT NULL, description TEXT NOT NULL, counterparty TEXT NOT NULL, dateEpochMillis INTEGER NOT NULL, receivedAtEpochMillis INTEGER NOT NULL)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pending_mpesa_transactions_reference ON pending_mpesa_transactions(reference)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_pending_mpesa_transactions_dateEpochMillis ON pending_mpesa_transactions(dateEpochMillis)")
            }
        }

        @Volatile private var INSTANCE: TrackItDatabase? = null

        fun getInstance(context: Context): TrackItDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, TrackItDatabase::class.java, "trackit_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

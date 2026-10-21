package com.inventsolutions.trackit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MoneyTransaction::class, MonthlyBudget::class],
    version = 2,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao

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

        @Volatile
        private var INSTANCE: TrackItDatabase? = null

        fun getInstance(context: Context): TrackItDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackItDatabase::class.java,
                    "trackit_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

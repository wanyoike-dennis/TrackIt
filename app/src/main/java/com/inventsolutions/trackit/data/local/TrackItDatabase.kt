package com.inventsolutions.trackit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MoneyTransaction::class],
    version = 1,
    exportSchema = false
)
abstract class TrackItDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: TrackItDatabase? = null

        fun getInstance(context: Context): TrackItDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrackItDatabase::class.java,
                    "trackit_database"
                ).build().also { INSTANCE = it }
            }
    }
}

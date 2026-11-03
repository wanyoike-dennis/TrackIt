package com.inventsolutions.trackit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.inventsolutions.trackit.data.local.AccountTransfer
import com.inventsolutions.trackit.data.local.FinancialAccount
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.TrackItBackupManager
import com.inventsolutions.trackit.data.local.TrackItDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialAccountBackupInstrumentedTest {

    private lateinit var database: TrackItDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TrackItDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun accountLinkedFinancialMovementsAndTransfersAreRestored() = runBlocking {
        val accountId = database.financialAccountDao().insert(
            FinancialAccount(
                name = "M-Pesa",
                type = FinancialAccount.MPESA,
                openingBalanceMinor = 1_000_000L,
                openingDateEpochMillis = 1L
            )
        )
        val destinationId = database.financialAccountDao().insert(
            FinancialAccount(
                name = "Bank",
                type = FinancialAccount.BANK,
                openingBalanceMinor = 0L,
                openingDateEpochMillis = 1L
            )
        )
        val savingsId = database.financialRecordDao().insert(
            FinancialRecord(
                type = FinancialRecord.SAVINGS_GOAL,
                title = "Emergency fund",
                targetMinor = 500_000L,
                currentMinor = 300_000L
            )
        )
        val debtId = database.financialRecordDao().insert(
            FinancialRecord(
                type = FinancialRecord.DEBT_I_OWE,
                title = "Loan",
                targetMinor = 200_000L,
                currentMinor = 50_000L
            )
        )

        database.transactionDao().insert(
            MoneyTransaction(
                type = MoneyTransaction.INCOME,
                amountMinor = 200_000L,
                category = "Salary",
                description = "Pay",
                paymentMethod = "M-Pesa",
                dateEpochMillis = 10L,
                accountId = accountId
            )
        )
        database.transactionDao().insert(
            MoneyTransaction(
                type = MoneyTransaction.EXPENSE,
                amountMinor = 50_000L,
                category = "Food",
                description = "Shopping",
                paymentMethod = "M-Pesa",
                dateEpochMillis = 11L,
                accountId = accountId
            )
        )
        database.financialRecordDao().insertEntry(
            FinancialRecordEntry(
                financialRecordId = savingsId,
                entryType = FinancialRecordEntry.CONTRIBUTION,
                amountMinor = 100_000L,
                accountId = accountId
            )
        )
        database.financialRecordDao().insertEntry(
            FinancialRecordEntry(
                financialRecordId = savingsId,
                entryType = FinancialRecordEntry.WITHDRAWAL,
                amountMinor = 20_000L,
                accountId = accountId
            )
        )
        database.financialRecordDao().insertEntry(
            FinancialRecordEntry(
                financialRecordId = debtId,
                entryType = FinancialRecordEntry.REPAYMENT,
                amountMinor = 30_000L,
                accountId = accountId
            )
        )
        database.accountTransferDao().insert(
            AccountTransfer(
                fromAccountId = accountId,
                toAccountId = destinationId,
                amountMinor = 40_000L,
                dateEpochMillis = 12L
            )
        )

        val backup = TrackItBackupManager.export(database)
        database.openHelper.writableDatabase.execSQL("DELETE FROM account_transfers")
        database.openHelper.writableDatabase.execSQL("DELETE FROM financial_record_entries")
        database.openHelper.writableDatabase.execSQL("DELETE FROM transactions")
        database.openHelper.writableDatabase.execSQL("DELETE FROM financial_records")
        database.openHelper.writableDatabase.execSQL("DELETE FROM financial_accounts")

        val restoredRows = TrackItBackupManager.restore(database, backup)

        assertEquals(10, restoredRows)
        assertEquals(2, database.financialAccountDao().observeAll().firstForTest().size)
        assertEquals(1, database.accountTransferDao().observeAll().firstForTest().size)
        assertEquals(3, database.financialRecordDao().observeAllEntries().firstForTest().size)

        val restoredTransaction = database.openHelper.readableDatabase.query(
            "SELECT accountId FROM transactions WHERE category = 'Salary'"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
        assertEquals(accountId, restoredTransaction)

        val restoredEntryAccount = database.openHelper.readableDatabase.query(
            "SELECT accountId FROM financial_record_entries WHERE entryType = 'CONTRIBUTION'"
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
        assertEquals(accountId, restoredEntryAccount)
    }

    @Test
    fun restoreRejectsWrongAppWithoutDeletingExistingAccounts() = runBlocking {
        val accountId = database.financialAccountDao().insert(
            FinancialAccount(
                name = "Cash",
                type = FinancialAccount.CASH,
                openingBalanceMinor = 25_000L,
                openingDateEpochMillis = 1L
            )
        )

        try {
            TrackItBackupManager.restore(database, """{"app":"Other","formatVersion":1,"tables":{}}""")
            throw AssertionError("Restore should reject a backup from another app.")
        } catch (_: IllegalArgumentException) {
            // Expected: validation happens before the restore transaction starts.
        }

        val cursor = database.openHelper.readableDatabase.query(
            "SELECT id FROM financial_accounts WHERE id = ?",
            arrayOf(accountId.toString())
        )
        cursor.use { assertEquals(true, it.moveToFirst()) }
    }

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstForTest(): T =
        this.first()
}

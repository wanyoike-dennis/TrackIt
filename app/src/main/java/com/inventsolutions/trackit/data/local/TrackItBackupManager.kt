package com.inventsolutions.trackit.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, user-controlled JSON backup for TrackIt's local Room tables.
 * Restore validates the complete payload before replacing any local rows.
 */
object TrackItBackupManager {
    private const val FORMAT_VERSION = 1
    private val restoreOrder = listOf(
        "expense_categories",
        "monthly_budgets",
        "financial_records",
        "transactions",
        "recurring_transactions",
        "financial_record_entries"
    )
    private val tables = restoreOrder.toSet()

    fun export(database: TrackItDatabase): String {
        val sqlDb = database.openHelper.readableDatabase
        val root = JSONObject()
            .put("app", "TrackIt")
            .put("formatVersion", FORMAT_VERSION)
            .put("exportedAtEpochMillis", System.currentTimeMillis())
        val tableData = JSONObject()
        restoreOrder.forEach { table ->
            val rows = JSONArray()
            sqlDb.query("SELECT * FROM $table").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (index in 0 until cursor.columnCount) {
                        val name = cursor.getColumnName(index)
                        when (cursor.getType(index)) {
                            Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> row.put(name, cursor.getLong(index))
                            Cursor.FIELD_TYPE_FLOAT -> row.put(name, cursor.getDouble(index))
                            Cursor.FIELD_TYPE_BLOB -> row.put(
                                name,
                                JSONObject().put("__trackitBlobBase64", Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP))
                            )
                            else -> row.put(name, cursor.getString(index))
                        }
                    }
                    rows.put(row)
                }
            }
            tableData.put(table, rows)
        }
        root.put("tables", tableData)
        return root.toString(2)
    }

    fun restore(database: TrackItDatabase, backupJson: String): Int {
        val root = JSONObject(backupJson)
        require(root.optString("app") == "TrackIt") { "This file is not a TrackIt backup." }
        require(root.optInt("formatVersion", -1) == FORMAT_VERSION) { "This TrackIt backup version is not supported." }
        val data = root.optJSONObject("tables") ?: error("The backup is missing its data tables.")
        require(restoreOrder.all { data.optJSONArray(it) != null }) { "The backup is incomplete." }
        require(data.keys().asSequence().all { it in tables }) { "The backup contains an unknown table." }

        val sqlDb = database.openHelper.writableDatabase
        var restoredRows = 0
        sqlDb.beginTransaction()
        try {
            // Delete children before parents to respect foreign-key constraints.
            listOf("financial_record_entries", "recurring_transactions", "transactions", "financial_records", "monthly_budgets", "expense_categories")
                .forEach { sqlDb.delete(it, null, null) }

            restoreOrder.forEach { table ->
                val rows = data.getJSONArray(table)
                for (rowIndex in 0 until rows.length()) {
                    val row = rows.getJSONObject(rowIndex)
                    val values = ContentValues()
                    val columns = sqlDb.query("SELECT * FROM $table LIMIT 0").use { cursor ->
                        (0 until cursor.columnCount).map { cursor.getColumnName(it) }.toSet()
                    }
                    val keys = row.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        require(key in columns) { "Backup has an unexpected column in $table." }
                        val value = row.get(key)
                        when {
                            value == JSONObject.NULL -> values.putNull(key)
                            value is JSONObject && value.has("__trackitBlobBase64") ->
                                values.put(key, Base64.decode(value.getString("__trackitBlobBase64"), Base64.DEFAULT))
                            value is String -> values.put(key, value)
                            value is Int -> values.put(key, value)
                            value is Long -> values.put(key, value)
                            value is Double -> values.put(key, value)
                            value is Boolean -> values.put(key, if (value) 1 else 0)
                            else -> error("Backup contains an unsupported value.")
                        }
                    }
                    require(values.size() > 0) { "Backup contains an empty row." }
                    require(sqlDb.insertOrThrow(table, null, values) != -1L) { "Could not restore $table." }
                    restoredRows++
                }
            }
            sqlDb.setTransactionSuccessful()
        } finally {
            sqlDb.endTransaction()
        }
        return restoredRows
    }
}

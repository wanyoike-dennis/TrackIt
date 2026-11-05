package com.inventsolutions.trackit.data.local

import android.content.Context
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, user-controlled TrackIt backup.
 *
 * The .trackit file is JSON-based and contains all Room tables plus explicitly
 * approved local profile and app-preference values. Android permissions,
 * credentials, tokens and hardware identifiers are deliberately excluded.
 */
object TrackItBackupManager {
    private const val FORMAT_VERSION = 2
    private const val PROFILE_PREFS = "trackit_personal_profile"
    private const val APPEARANCE_PREFS = "trackit_appearance"

    private val profileKeys = listOf("full_name", "email", "phone", "location")
    private val appearanceKeys = listOf("theme_mode")

    private val restoreOrder = listOf(
        "expense_categories",
        "monthly_budgets",
        "financial_records",
        "financial_accounts",
        "transactions",
        "recurring_transactions",
        "financial_record_entries",
        "account_transfers"
    )
    private val requiredLegacyTables = setOf(
        "expense_categories", "monthly_budgets", "financial_records",
        "transactions", "financial_record_entries"
    )
    private val tables = restoreOrder.toSet()

    fun export(context: Context, database: TrackItDatabase): String {
        val appInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val root = JSONObject()
            .put("app", "TrackIt")
            .put("formatVersion", FORMAT_VERSION)
            .put("metadata", JSONObject()
                .put("exportedAtEpochMillis", System.currentTimeMillis())
                .put("appVersionName", appInfo.versionName ?: "unknown")
                .put("appVersionCode", if (android.os.Build.VERSION.SDK_INT >= 28) appInfo.longVersionCode else appInfo.versionCode.toLong())
                .put("databaseVersion", database.openHelper.readableDatabase.version)
                .put("platform", JSONObject()
                    .put("name", "Android")
                    .put("apiLevel", android.os.Build.VERSION.SDK_INT)
                )
            )
            .put("profile", readPreferences(context, PROFILE_PREFS, profileKeys))
            .put("settings", JSONObject().put(
                "appearance",
                readPreferences(context, APPEARANCE_PREFS, appearanceKeys)
            ))

        val sqlDb = database.openHelper.readableDatabase
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

    fun restore(context: Context, database: TrackItDatabase, backupJson: String): Int {
        val root = JSONObject(backupJson)
        require(root.optString("app") == "TrackIt") { "This file is not a TrackIt backup." }
        val formatVersion = root.optInt("formatVersion", -1)
        require(formatVersion in 1..FORMAT_VERSION) { "This TrackIt backup version is not supported." }

        val data = root.optJSONObject("tables") ?: error("The backup is missing its data tables.")
        require(requiredLegacyTables.all { data.optJSONArray(it) != null }) { "The backup is incomplete." }
        require(data.has("recurring_transactions").not() || data.optJSONArray("recurring_transactions") != null) {
            "The recurring transaction data is malformed."
        }
        require(data.keys().asSequence().all { it in tables }) { "The backup contains an unknown table." }
        if (formatVersion >= 2) {
            require(tables.all { data.optJSONArray(it) != null }) { "The backup is missing one or more financial data tables." }
            val profile = root.optJSONObject("profile") ?: error("The backup is missing profile information.")
            val appearance = root.optJSONObject("settings")?.optJSONObject("appearance")
                ?: error("The backup is missing app configuration.")
            validatePreferences(profile, profileKeys)
            validatePreferences(appearance, appearanceKeys, mapOf("theme_mode" to setOf("system", "light", "dark")))
        }

        val sqlDb = database.openHelper.writableDatabase
        var restoredRows = 0
        sqlDb.beginTransaction()
        try {
            listOf(
                "account_transfers", "financial_record_entries", "recurring_transactions",
                "transactions", "financial_records", "monthly_budgets",
                "expense_categories", "financial_accounts"
            ).forEach { table ->
                if (table != "recurring_transactions" || data.has(table)) {
                    sqlDb.delete(table, null, null)
                }
            }

            restoreOrder.forEach { table ->
                val rows = data.optJSONArray(table) ?: return@forEach
                val columns = sqlDb.query("SELECT * FROM $table LIMIT 0").use { cursor ->
                    (0 until cursor.columnCount).map { cursor.getColumnName(it) }.toSet()
                }
                for (rowIndex in 0 until rows.length()) {
                    val row = rows.getJSONObject(rowIndex)
                    val values = ContentValues()
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
                    require(sqlDb.insert(table, SQLiteDatabase.CONFLICT_ABORT, values) != -1L) {
                        "Could not restore $table."
                    }
                    restoredRows++
                }
            }
            sqlDb.setTransactionSuccessful()
        } finally {
            sqlDb.endTransaction()
        }

        // Version 1 backups did not contain profile or app preferences.
        // Keep the current values when importing those older files.
        if (formatVersion >= 2) {
            require(writePreferences(context, PROFILE_PREFS, profileKeys, root.getJSONObject("profile"))) {
                "Financial records were restored, but profile information could not be saved."
            }
            val appearance = root.getJSONObject("settings").getJSONObject("appearance")
            require(writePreferences(context, APPEARANCE_PREFS, appearanceKeys, appearance)) {
                "Financial records were restored, but app appearance settings could not be saved."
            }
        }
        return restoredRows
    }

    private fun validatePreferences(
        values: JSONObject,
        allowedKeys: List<String>,
        allowedValues: Map<String, Set<String>> = emptyMap()
    ) {
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            require(key in allowedKeys) { "Backup contains an unsupported app preference." }
            val value = values.opt(key)
            require(value is String) { "Backup contains an invalid app preference value." }
            allowedValues[key]?.let { permitted ->
                require(value in permitted) { "Backup contains an unsupported appearance setting." }
            }
        }
    }

    private fun readPreferences(context: Context, preferenceName: String, allowedKeys: List<String>): JSONObject {
        val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
        val result = JSONObject()
        allowedKeys.forEach { key ->
            if (preferences.contains(key)) {
                when (val value = preferences.all[key]) {
                    is String -> result.put(key, value)
                    is Boolean -> result.put(key, value)
                    is Int -> result.put(key, value)
                    is Long -> result.put(key, value)
                    is Float -> result.put(key, value.toDouble())
                    is Set<*> -> result.put(key, JSONArray(value.filterIsInstance<String>()))
                }
            }
        }
        return result
    }

    private fun writePreferences(
        context: Context,
        preferenceName: String,
        allowedKeys: List<String>,
        values: JSONObject
    ): Boolean {
        val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        allowedKeys.forEach { key ->
            editor.remove(key)
            if (values.has(key) && !values.isNull(key)) {
                when (val value = values.get(key)) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is JSONArray -> {
                        val entries = buildSet {
                            for (index in 0 until value.length()) add(value.getString(index))
                        }
                        editor.putStringSet(key, entries)
                    }
                    else -> error("Backup contains an unsupported preference value.")
                }
            }
        }
        return editor.commit()
    }
}

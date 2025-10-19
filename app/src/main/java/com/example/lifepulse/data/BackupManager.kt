package com.example.lifepulse.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject   // ✅ Import JSON library
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object BackupManager {
    private const val PREFS_NAME = "LifePulsePrefs"

    // ✅ Export SharedPreferences to JSON
    fun backupData(context: Context, uri: Uri) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allData = prefs.all

        // Convert Map<String, *> to JSONObject properly
        val json = JSONObject()
        for ((key, value) in allData) {
            json.put(key, value)
        }

        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write(json.toString(4)) // pretty-print JSON
            }
        }
    }

    //  Import JSON into SharedPreferences
    fun restoreData(context: Context, uri: Uri) {
        val jsonStr = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    jsonStr.append(line)
                }
            }
        }

        val json = JSONObject(jsonStr.toString())
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        prefs.clear()

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = json.get(key)
            when (value) {
                is Boolean -> prefs.putBoolean(key, value)
                is Int -> prefs.putInt(key, value)
                is Float -> prefs.putFloat(key, value)
                is Long -> prefs.putLong(key, value)
                else -> prefs.putString(key, value.toString())
            }
        }
        prefs.apply()
    }
}

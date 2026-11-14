package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.LauncherSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "tilde_launcher_settings")

class SettingsDataStore(private val context: Context) {
    private val settingsJsonKey = stringPreferencesKey("launcher_settings_json")

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(LauncherSettings::class.java)

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs: Preferences ->
            val raw = prefs[settingsJsonKey]
            if (raw.isNullOrBlank()) {
                LauncherSettings()
            } else {
                runCatching { adapter.fromJson(raw) }.getOrNull() ?: LauncherSettings()
            }
        }

    suspend fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs[settingsJsonKey]?.let {
                runCatching { adapter.fromJson(it) }.getOrNull()
            } ?: LauncherSettings()
            val updated = transform(current)
            prefs[settingsJsonKey] = adapter.toJson(updated)
        }
    }

    suspend fun replaceSettings(newSettings: LauncherSettings) {
        context.dataStore.edit { prefs ->
            prefs[settingsJsonKey] = adapter.toJson(newSettings)
        }
    }

    fun exportToJson(settings: LauncherSettings): String {
        return adapter.indent("  ").toJson(settings)
    }

    fun importFromJson(json: String): LauncherSettings? {
        return runCatching { adapter.fromJson(json) }.getOrNull()
    }
}

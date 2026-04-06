package com.jerecipes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jerecipes_settings")

data class AppSettings(
    val customApiKey: String = "",
    val customPrompt: String = ""
)

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_API_KEY = stringPreferencesKey("custom_api_key")
        private val KEY_CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            customApiKey = prefs[KEY_API_KEY] ?: "",
            customPrompt = prefs[KEY_CUSTOM_PROMPT] ?: ""
        )
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setCustomPrompt(prompt: String) {
        context.dataStore.edit { it[KEY_CUSTOM_PROMPT] = prompt }
    }
}

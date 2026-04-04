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
    val modelName: String = GeminiModel.FLASH_LITE.id,
    val customApiKey: String = "",
    val customPrompt: String = ""
)

enum class GeminiModel(val id: String, val displayName: String) {
    FLASH_LITE("gemini-3.1-flash-lite-preview", "Flash Lite (faster)"),
    FLASH("gemini-3-flash-preview", "Flash (more capable)")
}

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_MODEL = stringPreferencesKey("model_name")
        private val KEY_API_KEY = stringPreferencesKey("custom_api_key")
        private val KEY_CUSTOM_PROMPT = stringPreferencesKey("custom_prompt")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            modelName = prefs[KEY_MODEL] ?: GeminiModel.FLASH_LITE.id,
            customApiKey = prefs[KEY_API_KEY] ?: "",
            customPrompt = prefs[KEY_CUSTOM_PROMPT] ?: ""
        )
    }

    suspend fun setModel(modelId: String) {
        context.dataStore.edit { it[KEY_MODEL] = modelId }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setCustomPrompt(prompt: String) {
        context.dataStore.edit { it[KEY_CUSTOM_PROMPT] = prompt }
    }
}

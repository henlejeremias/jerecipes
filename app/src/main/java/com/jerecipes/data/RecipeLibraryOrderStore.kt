package com.jerecipes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recipeLibraryOrderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recipe_library_order"
)

private object RecipeLibraryOrderKeys {
    val IDS_CSV = stringPreferencesKey("recipe_ids_order")
}

class RecipeLibraryOrderStore(context: Context) {
    private val dataStore = context.applicationContext.recipeLibraryOrderDataStore

    val orderFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[RecipeLibraryOrderKeys.IDS_CSV]
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    suspend fun saveOrder(ids: List<String>) {
        dataStore.edit { prefs ->
            prefs[RecipeLibraryOrderKeys.IDS_CSV] = ids.joinToString(",")
        }
    }
}

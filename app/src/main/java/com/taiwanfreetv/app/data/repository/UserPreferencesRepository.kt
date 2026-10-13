package com.taiwanfreetv.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITE_CHANNEL_IDS = stringSetPreferencesKey("favorite_channel_ids")
        val LAST_WATCHED_CHANNEL_ID = stringPreferencesKey("last_watched_channel_id")
        val CUSTOM_CHANNEL_ORDER = stringPreferencesKey("custom_channel_order") // JSON string list of channel IDs
    }

    val favoriteChannelIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FAVORITE_CHANNEL_IDS] ?: emptySet()
    }

    val lastWatchedChannelId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_WATCHED_CHANNEL_ID]
    }

    val customChannelOrder: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.CUSTOM_CHANNEL_ORDER]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            raw.split(",")
        }
    }

    suspend fun toggleFavorite(channelId: String) {
        context.dataStore.edit { preferences ->
            val currentFavorites = preferences[PreferencesKeys.FAVORITE_CHANNEL_IDS]?.toMutableSet() ?: mutableSetOf()
            if (currentFavorites.contains(channelId)) {
                currentFavorites.remove(channelId)
            } else {
                currentFavorites.add(channelId)
            }
            preferences[PreferencesKeys.FAVORITE_CHANNEL_IDS] = currentFavorites
        }
    }

    suspend fun setLastWatchedChannelId(channelId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_WATCHED_CHANNEL_ID] = channelId
        }
    }

    suspend fun saveCustomChannelOrder(channelIds: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_CHANNEL_ORDER] = channelIds.joinToString(",")
        }
    }
}

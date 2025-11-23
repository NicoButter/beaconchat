package com.nicobutter.beaconchat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val CALLSIGN_KEY = stringPreferencesKey("callsign")
        private val CALLSIGN_ENABLED_KEY = booleanPreferencesKey("callsign_enabled")

        const val MIN_CALLSIGN_LENGTH = 3
        const val MAX_CALLSIGN_LENGTH = 8
    }

    // Flow to observe callsign
    val callsign: Flow<String> =
            context.dataStore.data.map { preferences -> preferences[CALLSIGN_KEY] ?: "" }

    // Flow to observe if callsign is enabled
    val callsignEnabled: Flow<Boolean> =
            context.dataStore.data.map { preferences -> preferences[CALLSIGN_ENABLED_KEY] ?: false }

    // Save callsign
    suspend fun saveCallsign(callsign: String) {
        context.dataStore.edit { preferences ->
            preferences[CALLSIGN_KEY] = callsign.uppercase().trim()
        }
    }

    // Set callsign enabled/disabled
    suspend fun setCallsignEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[CALLSIGN_ENABLED_KEY] = enabled }
    }

    // Validate callsign
    fun isValidCallsign(callsign: String): Boolean {
        val trimmed = callsign.trim()
        return trimmed.length in MIN_CALLSIGN_LENGTH..MAX_CALLSIGN_LENGTH &&
                trimmed.all { it.isLetterOrDigit() }
    }

    // Get formatted message with callsign
    suspend fun formatMessageWithCallsign(
            message: String,
            currentCallsign: String,
            isEnabled: Boolean
    ): String {
        return if (isEnabled && currentCallsign.isNotBlank()) {
            "${currentCallsign.uppercase()}-$message"
        } else {
            message
        }
    }
}

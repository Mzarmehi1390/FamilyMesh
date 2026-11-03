package com.familymesh.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class LocalProfile(
    val deviceId: String,
    val displayName: String
)

/**
 * Stores the identity shown on this device only.
 *
 * This is not an account, cryptographic identity, or network identity.
 */
class LocalProfileRepository(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val deviceId = stringPreferencesKey("local_device_id")
        val displayName = stringPreferencesKey("local_display_name")
    }

    val profile: Flow<LocalProfile> = dataStore.data.map { preferences ->
        LocalProfile(
            deviceId = preferences[Keys.deviceId].orEmpty(),
            displayName = preferences[Keys.displayName] ?: DEFAULT_DISPLAY_NAME
        )
    }

    suspend fun initialize() {
        dataStore.edit { preferences ->
            if (preferences[Keys.deviceId].isNullOrBlank()) {
                preferences[Keys.deviceId] = UUID.randomUUID().toString()
            }
            if (preferences[Keys.displayName].isNullOrBlank()) {
                preferences[Keys.displayName] = DEFAULT_DISPLAY_NAME
            }
        }
    }

    suspend fun updateDisplayName(value: String) {
        val normalized = value.trim().take(MAX_DISPLAY_NAME_LENGTH)
        require(normalized.isNotBlank()) { "Display name must not be blank." }
        dataStore.edit { preferences ->
            preferences[Keys.displayName] = normalized
        }
    }

    private companion object {
        const val DEFAULT_DISPLAY_NAME = "Family member"
        const val MAX_DISPLAY_NAME_LENGTH = 40
    }
}

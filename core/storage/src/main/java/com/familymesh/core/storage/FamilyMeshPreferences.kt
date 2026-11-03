package com.familymesh.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile

class FamilyMeshPreferences(context: Context) {
    val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            context.applicationContext.preferencesDataStoreFile("familymesh.preferences_pb")
        }
    )
}

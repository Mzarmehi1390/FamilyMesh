package com.familymesh.app.di

import com.familymesh.core.common.domain.FamilySpaceRepository
import com.familymesh.core.storage.FamilyMeshPreferences
import com.familymesh.core.storage.LocalProfileRepository
import com.familymesh.core.storage.family.FamilyMeshDatabase
import com.familymesh.core.storage.family.RoomFamilySpaceRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { FamilyMeshPreferences(androidContext()) }
    single { LocalProfileRepository(get<FamilyMeshPreferences>().dataStore) }
    single { FamilyMeshDatabase.create(androidContext()) }
    single<FamilySpaceRepository> {
        RoomFamilySpaceRepository(get<FamilyMeshDatabase>().familySpaceDao())
    }
}

package com.familymesh.app.di

import com.familymesh.core.storage.FamilyMeshPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { FamilyMeshPreferences(androidContext()) }
}

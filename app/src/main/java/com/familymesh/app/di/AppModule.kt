package com.familymesh.app.di

import com.familymesh.core.common.domain.FamilySpaceRepository
import com.familymesh.core.common.domain.EnrollmentNonceStore
import com.familymesh.core.storage.FamilyMeshPreferences
import com.familymesh.core.storage.LocalProfileRepository
import com.familymesh.core.storage.family.FamilyMeshDatabase
import com.familymesh.core.storage.family.RoomFamilySpaceRepository
import com.familymesh.core.storage.family.RoomEnrollmentNonceStore
import com.familymesh.core.identity.DeviceEnrollmentProofAcceptanceService
import com.familymesh.core.identity.AndroidKeystoreDeviceKeyManager
import com.familymesh.core.identity.DeviceKeyManager
import com.familymesh.core.identity.DeviceKeySecurityInfoProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { AndroidKeystoreDeviceKeyManager(androidContext()) }
    single<DeviceKeyManager> { get<AndroidKeystoreDeviceKeyManager>() }
    single<DeviceKeySecurityInfoProvider> { get<AndroidKeystoreDeviceKeyManager>() }
    single { FamilyMeshPreferences(androidContext()) }
    single { LocalProfileRepository(get<FamilyMeshPreferences>().dataStore) }
    single { FamilyMeshDatabase.create(androidContext()) }
    single<EnrollmentNonceStore> { RoomEnrollmentNonceStore(get<FamilyMeshDatabase>().enrollmentNonceDao()) }
    single { DeviceEnrollmentProofAcceptanceService(get()) }
    single<FamilySpaceRepository> {
        RoomFamilySpaceRepository(get<FamilyMeshDatabase>().familySpaceDao())
    }
}

package com.familymesh.app

import android.app.Application
import com.familymesh.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FamilyMeshApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FamilyMeshApplication)
            modules(appModule)
        }
    }
}

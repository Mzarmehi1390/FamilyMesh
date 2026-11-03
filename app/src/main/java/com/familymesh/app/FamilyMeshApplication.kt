package com.familymesh.app

import android.app.Application
import com.familymesh.app.di.appModule
import com.familymesh.core.storage.LocalProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FamilyMeshApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FamilyMeshApplication)
            modules(appModule)
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            get<LocalProfileRepository>().initialize()
        }
    }
}

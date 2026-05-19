package com.kmpbits.sample.android

import android.app.Application
import com.kmpbits.sample.android.core.di.databaseModule
import com.kmpbits.sample.android.core.di.networkModule
import com.kmpbits.sample.android.core.di.repositoryModule
import com.kmpbits.sample.android.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                networkModule,
                repositoryModule,
                viewModelModule,
                databaseModule
            )
        }
    }
}
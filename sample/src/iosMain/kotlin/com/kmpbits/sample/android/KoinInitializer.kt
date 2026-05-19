package com.kmpbits.sample.android

import com.kmpbits.sample.android.core.di.databaseModule
import com.kmpbits.sample.android.core.di.networkModule
import com.kmpbits.sample.android.core.di.repositoryModule
import com.kmpbits.sample.android.core.di.viewModelModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(
            databaseModule,
            networkModule,
            repositoryModule,
            viewModelModule
        )
    }
}

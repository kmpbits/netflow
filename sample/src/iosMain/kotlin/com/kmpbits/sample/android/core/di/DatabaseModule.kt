package com.kmpbits.sample.android.core.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kmpbits.sample.android.data.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module
import platform.Foundation.NSHomeDirectory

val databaseModule = module {
    single {
        val dbPath = NSHomeDirectory() + "/app_database.db"
        Room.databaseBuilder<AppDatabase>(name = dbPath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
    single { get<AppDatabase>().todoDao() }
}

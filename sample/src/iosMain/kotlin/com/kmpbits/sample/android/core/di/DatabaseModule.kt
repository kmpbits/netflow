package com.kmpbits.sample.android.core.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.kmpbits.sample.android.data.database.AppDatabase
import org.koin.dsl.module

val databaseModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(AppDatabase.Schema, "app_database.db")
    }
    single { AppDatabase(get()) }
}

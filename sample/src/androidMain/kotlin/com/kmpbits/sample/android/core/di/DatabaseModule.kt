package com.kmpbits.sample.android.core.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kmpbits.sample.android.data.database.AppDatabase
import org.koin.dsl.module

val databaseModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(AppDatabase.Schema, get(), "app_database.db")
    }
    single { AppDatabase(get()) }
}

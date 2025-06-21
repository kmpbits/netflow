package com.kmpbits.sample.android.core.di

import android.content.Context
import androidx.room.Room
import com.kmpbits.sample.android.data.database.AppDatabase
import org.koin.dsl.module

val databaseModule = module {
    fun createDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }

    single { createDatabase(get()) }
}
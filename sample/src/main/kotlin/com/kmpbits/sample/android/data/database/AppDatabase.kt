package com.kmpbits.sample.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kmpbits.sample.android.data.database.dao.TodoDao
import com.kmpbits.sample.android.data.dto.TodoDto

@Database(entities = [TodoDto::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
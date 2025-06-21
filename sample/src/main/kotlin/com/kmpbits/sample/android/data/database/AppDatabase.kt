package com.kmpbits.sample.android.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kmpbits.sample.android.data.database.dao.TodoDao
import com.kmpbits.sample.android.data.database.entity.TodoEntity

@Database(entities = [TodoEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
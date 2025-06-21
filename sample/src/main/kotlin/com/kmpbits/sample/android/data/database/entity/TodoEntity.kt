package com.kmpbits.sample.android.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: Int,
    val userId: Int,
    val title: String,
    val completed: Boolean,
    val addedTimestamp: Long = System.currentTimeMillis()
)

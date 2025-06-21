package com.kmpbits.sample.android.data.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "todos")
data class TodoDto(
    val userId: Int,
    @PrimaryKey
    val id: Int,
    val title: String,
    val completed: Boolean
)

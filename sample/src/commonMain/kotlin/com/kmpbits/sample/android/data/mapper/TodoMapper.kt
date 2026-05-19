package com.kmpbits.sample.android.data.mapper

import com.kmpbits.sample.android.data.database.TodoEntity
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.domain.model.Todo

fun TodoDto.toModel() = Todo(
    id = id,
    userId = userId,
    title = title,
    completed = completed
)

fun TodoEntity.toModel() = Todo(
    id = id.toInt(),
    userId = userId.toInt(),
    title = title,
    completed = completed
)

fun TodoEntity.toDto() = TodoDto(
    id = id.toInt(),
    userId = userId.toInt(),
    title = title,
    completed = completed
).also {
    it.page = page.toInt()
    it.lastUpdatedTimestamp = lastUpdatedTimestamp
}

fun TodoDto.toEntity() = TodoEntity(
    id = id.toLong(),
    userId = userId.toLong(),
    title = title,
    completed = completed,
    page = page.toLong(),
    lastUpdatedTimestamp = lastUpdatedTimestamp
)

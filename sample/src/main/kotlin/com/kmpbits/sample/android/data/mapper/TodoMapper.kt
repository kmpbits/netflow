package com.kmpbits.sample.android.data.mapper

import com.kmpbits.sample.android.data.database.entity.TodoEntity
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.domain.model.Todo

fun TodoDto.toModel() = Todo(
    id = id,
    userId = userId,
    title = title,
    completed = completed
)

fun TodoEntity.toDto() = TodoDto(
    id = id,
    userId = userId,
    title = title,
    completed = completed
)

fun TodoDto.toEntity() = TodoEntity(
    id = id,
    userId = userId,
    title = title,
    completed = completed
)
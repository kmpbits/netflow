package com.kmpbits.sample.android.data.mapper

import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.domain.model.Todo

fun TodoDto.toModel() = Todo(
    id = id,
    userId = userId,
    title = title,
    completed = completed
)
package com.kmpbits.sample.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTodoRequest(
    val title: String,
    val completed: Boolean,
)

package com.kmpbits.netflow_core.response

import com.kmpbits.netflow_core.enums.ErrorResponseType

data class ErrorResponse(
    val code: Int,
    val errorBody: String?,
    val type: ErrorResponseType
)

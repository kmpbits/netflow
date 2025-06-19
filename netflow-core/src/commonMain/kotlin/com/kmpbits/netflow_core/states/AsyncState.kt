package com.kmpbits.netflow_core.states

import com.kmpbits.netflow_core.response.ErrorResponse

sealed class AsyncState<out T> {
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val error: ErrorResponse) : AsyncState<Nothing>()
    object Empty : AsyncState<Nothing>()
}

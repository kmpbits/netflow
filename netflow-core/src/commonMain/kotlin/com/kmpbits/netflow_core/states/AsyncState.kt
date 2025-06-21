package com.kmpbits.netflow_core.states

import com.kmpbits.netflow_core.response.ErrorResponse

sealed class AsyncState<out T> {
    data class Success<T>(val data: T) : AsyncState<T>()
    data class Error(val error: ErrorResponse) : AsyncState<Nothing>()
    data object Empty : AsyncState<Nothing>()
}

/**
 * This function is to map the data inside the [AsyncState]
 * This is usually used in the repository to map the dto to the model
 */
fun <T, E> AsyncState<T>.map(transform: (T) -> E): AsyncState<E> {
    return when (this) {
        is AsyncState.Error -> AsyncState.Error(error)
        is AsyncState.Empty -> AsyncState.Empty
        is AsyncState.Success -> AsyncState.Success(transform(data))
    }
}
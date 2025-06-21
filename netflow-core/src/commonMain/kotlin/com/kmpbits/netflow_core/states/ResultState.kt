package com.kmpbits.netflow_core.states

import com.kmpbits.netflow_core.response.ErrorResponse

sealed class ResultState<out T> {
    data class Error<T>(val error: ErrorResponse, val data: T? = null) : ResultState<T>()
    data object Loading : ResultState<Nothing>()
    data object Empty : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
}

/**
 * This function is to map the data inside the [ResultState]
 * This is usually used in the repository to map the dto to the model
 */
fun <T, E> ResultState<T>.map(transform: (T) -> E): ResultState<E> {
    return when (this) {
        is ResultState.Error -> ResultState.Error(error, data?.let { transform(it) })
        is ResultState.Loading -> ResultState.Loading
        is ResultState.Empty -> ResultState.Empty
        is ResultState.Success -> ResultState.Success(transform(data))
    }
}

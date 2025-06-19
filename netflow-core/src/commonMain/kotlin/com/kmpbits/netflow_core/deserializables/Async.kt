package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.builders.ResponseBuilder
import com.kmpbits.netflow_core.enums.ErrorResponseType
import com.kmpbits.netflow_core.exceptions.toError
import com.kmpbits.netflow_core.extensions.toEnvelopeList
import com.kmpbits.netflow_core.extensions.toList
import com.kmpbits.netflow_core.extensions.toModel
import com.kmpbits.netflow_core.extensions.toModelWrapped
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_core.response.ErrorResponse
import com.kmpbits.netflow_core.response.NetFlowResponse
import com.kmpbits.netflow_core.states.AsyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Deserialize the request into a [AsyncState].
 *
 * This method receives a [ResponseBuilder] as parameter to customize the response.
 *
 * This is a suspend functions, so you need to call it from a coroutine or from another suspend function.
 * The dispatcher is handled by the function, so it can be called from any thread.
 *
 * @return [AsyncState] with success (with data as [T]) or error (with error as [ErrorResponse]).
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseAsync(
    crossinline responseBuilder: ResponseBuilder<T>. () -> Unit = {}
): AsyncState<T> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModel<T>() }
    )
}

/**
 * Deserialize the request into a [AsyncState] list.
 *
 * This method receives a [ResponseBuilder] as parameter to customize the response.
 *
 * This is a suspend functions, so you need to call it from a coroutine or from another suspend function.
 * The dispatcher is handled by the function, so it can be called from any thread.
 *
 * @return [AsyncState] with success (with data as [List] of type [T]) or error (with error as [ErrorResponse]).
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseListAsync(
    crossinline responseBuilder: ResponseBuilder<List<T>>. () -> Unit = {}
): AsyncState<List<T>> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toList() }
    )
}

/**
 * Deserialize the request into a [AsyncState] list wrapped by the data json object.
 * The list wrapped in json object response should be called "data"
 *
 * This method receives a [ResponseBuilder] as parameter to customize the response.
 *
 * This is a suspend functions, so you need to call it from a coroutine or from another suspend function.
 * The dispatcher is handled by the function, so it can be called from any thread.
 *
 * @return [AsyncState] with success (with data as [List] of type [T]) or error (with error as [ErrorResponse]).
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseWrappedListAsync(
    crossinline responseBuilder: ResponseBuilder<List<T>>. () -> Unit = {}
): AsyncState<List<T>> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toEnvelopeList<T>().data }
    )
}

/**
 * Deserialize the request into a [AsyncState] wrapped by the data json object.
 * The object wrapped in json object response should be called "data"
 *
 * This method receives a [ResponseBuilder] as parameter to customize the response.
 *
 * This is a suspend functions, so you need to call it from a coroutine or from another suspend function.
 * The dispatcher is handled by the function, so it can be called from any thread.
 *
 * @return [AsyncState] with success (with data as [T]) or error (with error as [ErrorResponse]).
 */
suspend inline fun <reified T: Any> NetFlowRequest.responseWrappedAsync(
    crossinline responseBuilder: ResponseBuilder<T>. () -> Unit = {}
): AsyncState<T> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModelWrapped<T>() }
    )
}

@PublishedApi
internal suspend inline fun <reified T: Any> NetFlowRequest.toAsync(
    crossinline responseBuilder: ResponseBuilder<T>. () -> Unit,
    deserializeBlock: (NetFlowResponse) -> T?
): AsyncState<T> {
    val response = ResponseBuilder<T>().also(responseBuilder)

    immutableRequestBuilder.preCall?.invoke()

    val callResponse = response()

    response.post?.invoke()

    return if (callResponse.isSuccess) {
        val result = deserializeBlock(callResponse)
        result?.let {
            withContext(Dispatchers.IO) { response.onNetworkSuccess?.invoke(it) }
            AsyncState.Success(it)
        } ?: AsyncState.Error(ErrorResponse(404, "Not found", ErrorResponseType.Empty))

    } else {
        AsyncState.Error(callResponse.toError())
    }
}
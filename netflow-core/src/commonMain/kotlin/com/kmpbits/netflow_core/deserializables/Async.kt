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
 * Deserialize the request into an [AsyncState].
 *
 * Use when [ApiType] and [DisplayType] are the same. For different types use the overload
 * that requires a [transform] parameter.
 *
 * Set [ResponseBuilder.wrappedResponse] to true when the API returns `{ "data": ... }`.
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseAsync(
    crossinline responseBuilder: ResponseBuilder<T, T>.() -> Unit = {}
): AsyncState<T> {
    val wrapped = ResponseBuilder<T, T>().also(responseBuilder).wrappedResponse
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { if (wrapped) it.toModelWrapped<T>() else it.toModel<T>() }
    )
}

/**
 * Deserialize the request into an [AsyncState].
 *
 * [ApiType] is the type the API response is deserialized into.
 * [DisplayType] is the type wrapped in [AsyncState].
 * [transform] maps [ApiType] to [DisplayType] and is required when the two types differ.
 *
 * Set [ResponseBuilder.wrappedResponse] to true when the API returns `{ "data": ... }`.
 */
suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.responseAsync(
    noinline transform: (ApiType) -> DisplayType,
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {}
): AsyncState<DisplayType> {
    val wrapped = ResponseBuilder<ApiType, DisplayType>().also(responseBuilder).wrappedResponse
    return toAsync(
        responseBuilder = {
            responseBuilder()
            apiTransform = transform
        },
        deserializeBlock = { if (wrapped) it.toModelWrapped<ApiType>() else it.toModel<ApiType>() }
    )
}

/**
 * Deserialize the request into an [AsyncState] list.
 *
 * Use when [ApiItem] and [DisplayItem] are the same. For different types use the overload
 * that requires a [transform] parameter.
 *
 * Set [ResponseBuilder.wrappedResponse] to true when the API returns `{ "data": [...] }`.
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseListAsync(
    crossinline responseBuilder: ResponseBuilder<List<T>, List<T>>.() -> Unit = {}
): AsyncState<List<T>> {
    val wrapped = ResponseBuilder<List<T>, List<T>>().also(responseBuilder).wrappedResponse
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { if (wrapped) it.toEnvelopeList<T>().data else it.toList() }
    )
}

/**
 * Deserialize the request into an [AsyncState] list.
 *
 * [ApiItem] is the item type the API response is deserialized into.
 * [DisplayItem] is the item type wrapped in [AsyncState].
 * [transform] maps each [ApiItem] to [DisplayItem] and is required when the two types differ.
 *
 * Set [ResponseBuilder.wrappedResponse] to true when the API returns `{ "data": [...] }`.
 */
suspend inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowRequest.responseListAsync(
    noinline transform: (ApiItem) -> DisplayItem,
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {}
): AsyncState<List<DisplayItem>> {
    val wrapped = ResponseBuilder<List<ApiItem>, List<DisplayItem>>().also(responseBuilder).wrappedResponse
    return toAsync(
        responseBuilder = {
            responseBuilder()
            apiTransform = { list -> list.map(transform) }
        },
        deserializeBlock = { if (wrapped) it.toEnvelopeList<ApiItem>().data else it.toList() }
    )
}

/**
 * Deserialize the request into an [AsyncState] wrapped by the data json object.
 *
 * Use when [ApiType] and [DisplayType] are the same. For different types use the overload
 * that requires a [transform] parameter.
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseWrappedAsync(
    crossinline responseBuilder: ResponseBuilder<T, T>.() -> Unit = {}
): AsyncState<T> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModelWrapped<T>() }
    )
}

/**
 * Deserialize the request into an [AsyncState] wrapped by the data json object.
 *
 * [ApiType] is the type the API response is deserialized into.
 * [DisplayType] is the type wrapped in [AsyncState].
 * [transform] maps [ApiType] to [DisplayType] and is required when the two types differ.
 */
suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.responseWrappedAsync(
    noinline transform: (ApiType) -> DisplayType,
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {}
): AsyncState<DisplayType> {
    return toAsync(
        responseBuilder = {
            responseBuilder()
            apiTransform = transform
        },
        deserializeBlock = { it.toModelWrapped<ApiType>() }
    )
}

/**
 * Deserialize the request into an [AsyncState] list wrapped by the data json object.
 *
 * Use when [ApiItem] and [DisplayItem] are the same. For different types use the overload
 * that requires a [transform] parameter.
 */
suspend inline fun <reified T : Any> NetFlowRequest.responseWrappedListAsync(
    crossinline responseBuilder: ResponseBuilder<List<T>, List<T>>.() -> Unit = {}
): AsyncState<List<T>> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toEnvelopeList<T>().data }
    )
}

/**
 * Deserialize the request into an [AsyncState] list wrapped by the data json object.
 *
 * [ApiItem] is the item type the API response is deserialized into.
 * [DisplayItem] is the item type wrapped in [AsyncState].
 * [transform] maps each [ApiItem] to [DisplayItem] and is required when the two types differ.
 */
suspend inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowRequest.responseWrappedListAsync(
    noinline transform: (ApiItem) -> DisplayItem,
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {}
): AsyncState<List<DisplayItem>> {
    return toAsync(
        responseBuilder = {
            responseBuilder()
            apiTransform = { list -> list.map(transform) }
        },
        deserializeBlock = { it.toEnvelopeList<ApiItem>().data }
    )
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.toAsync(
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit,
    deserializeBlock: (NetFlowResponse) -> ApiType?
): AsyncState<DisplayType> {
    val response = ResponseBuilder<ApiType, DisplayType>().also(responseBuilder)

    immutableRequestBuilder.preCall?.invoke()

    val callResponse = response()

    response.post?.invoke()

    return if (callResponse.isSuccess) {
        val apiResult = if (ApiType::class == Unit::class && callResponse.body.isNullOrEmpty()) {
            @Suppress("UNCHECKED_CAST")
            Unit as ApiType
        } else {
            deserializeBlock(callResponse)
        }
        apiResult?.let {
            withContext(Dispatchers.IO) { response.onNetworkSuccess?.invoke(it) }
            val displayResult: DisplayType = response.apiTransform?.invoke(it) ?: it as DisplayType
            AsyncState.Success(displayResult)
        } ?: AsyncState.Error(ErrorResponse(404, "Not found", ErrorResponseType.Empty))
    } else {
        AsyncState.Error(callResponse.toError())
    }
}

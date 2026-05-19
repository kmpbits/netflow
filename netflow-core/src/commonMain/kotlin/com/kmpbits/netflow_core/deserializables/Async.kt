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
 * [ApiType] is the type the API response is deserialized into.
 * [DisplayType] is the type wrapped in [AsyncState] — use [ResponseBuilder.apiTransform] to map
 * between the two. When both are the same, specify the type once: `responseAsync<MyType, MyType>`.
 */
suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.responseAsync(
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {}
): AsyncState<DisplayType> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModel<ApiType>() }
    )
}

/**
 * Deserialize the request into an [AsyncState] list.
 *
 * [ApiItem] is the item type the API response is deserialized into.
 * [DisplayItem] is the item type wrapped in [AsyncState] — use [ResponseBuilder.apiTransform] to map between the two.
 */
suspend inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowRequest.responseListAsync(
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {}
): AsyncState<List<DisplayItem>> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toList() }
    )
}

/**
 * Deserialize the request into an [AsyncState] list wrapped by the data json object.
 * The list wrapped in json object response should be called "data".
 *
 * [ApiItem] is the item type the API response is deserialized into.
 * [DisplayItem] is the item type wrapped in [AsyncState] — use [ResponseBuilder.apiTransform] to map between the two.
 */
suspend inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowRequest.responseWrappedListAsync(
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {}
): AsyncState<List<DisplayItem>> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toEnvelopeList<ApiItem>().data }
    )
}

/**
 * Deserialize the request into an [AsyncState] wrapped by the data json object.
 * The object wrapped in json object response should be called "data".
 *
 * [ApiType] is the type the API response is deserialized into.
 * [DisplayType] is the type wrapped in [AsyncState] — use [ResponseBuilder.apiTransform] to map between the two.
 */
suspend inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.responseWrappedAsync(
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {}
): AsyncState<DisplayType> {
    return toAsync(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModelWrapped<ApiType>() }
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
        val apiResult = deserializeBlock(callResponse)
        apiResult?.let {
            withContext(Dispatchers.IO) { response.onNetworkSuccess?.invoke(it) }
            val displayResult: DisplayType = response.apiTransform?.invoke(it) ?: it as DisplayType
            AsyncState.Success(displayResult)
        } ?: AsyncState.Error(ErrorResponse(404, "Not found", ErrorResponseType.Empty))
    } else {
        AsyncState.Error(callResponse.toError())
    }
}

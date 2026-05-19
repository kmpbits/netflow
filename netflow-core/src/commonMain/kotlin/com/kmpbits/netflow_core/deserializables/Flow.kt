package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.builders.ResponseBuilder
import com.kmpbits.netflow_core.enums.ErrorResponseType
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.exceptions.toError
import com.kmpbits.netflow_core.extensions.toEnvelopeList
import com.kmpbits.netflow_core.extensions.toList
import com.kmpbits.netflow_core.extensions.toModel
import com.kmpbits.netflow_core.extensions.toModelWrapped
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_core.response.ErrorResponse
import com.kmpbits.netflow_core.response.NetFlowResponse
import com.kmpbits.netflow_core.states.ResultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Deserialize the request into a [Flow].
 *
 * [ApiType] is the type the API response is deserialized into.
 * [DisplayType] is the type emitted in [ResultState] — use [ResponseBuilder.apiTransform] to map
 * between the two. When both are the same, specify the type once: `responseFlow<MyType, MyType>`.
 */
inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.responseFlow(
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {}
): Flow<ResultState<DisplayType>> {
    return toFlow(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModel<ApiType>() }
    )
}

/**
 * Deserialize the request into a [Flow] wrapped by the data json object.
 * The object wrapped in json object response should be called "data".
 *
 * [ApiType] is the type the API response is deserialized into.
 * [DisplayType] is the type emitted in [ResultState] — use [ResponseBuilder.apiTransform] to map between the two.
 */
inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.responseWrappedFlow(
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit = {}
): Flow<ResultState<DisplayType>> {
    return toFlow(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toModelWrapped<ApiType>() }
    )
}

/**
 * Deserialize the request into a [Flow] list.
 *
 * [ApiItem] is the item type the API response is deserialized into.
 * [DisplayItem] is the item type emitted in [ResultState] — use [ResponseBuilder.apiTransform] to map between the two.
 */
inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowRequest.responseListFlow(
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {}
): Flow<ResultState<List<DisplayItem>>> {
    return toFlow(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toList() }
    )
}

/**
 * Deserialize the request into a [Flow] list wrapped by the data json object.
 *
 * [ApiItem] is the item type the API response is deserialized into.
 * [DisplayItem] is the item type emitted in [ResultState] — use [ResponseBuilder.apiTransform] to map between the two.
 */
inline fun <reified ApiItem : Any, DisplayItem : Any> NetFlowRequest.responseWrappedListFlow(
    crossinline responseBuilder: ResponseBuilder<List<ApiItem>, List<DisplayItem>>.() -> Unit = {}
): Flow<ResultState<List<DisplayItem>>> {
    return toFlow(
        responseBuilder = responseBuilder,
        deserializeBlock = { it.toEnvelopeList<ApiItem>().data }
    )
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal inline fun <reified ApiType : Any, DisplayType : Any> NetFlowRequest.toFlow(
    crossinline responseBuilder: ResponseBuilder<ApiType, DisplayType>.() -> Unit,
    crossinline deserializeBlock: (NetFlowResponse) -> ApiType?
): Flow<ResultState<DisplayType>> = channelFlow {
    val response = ResponseBuilder<ApiType, DisplayType>().also(responseBuilder)

    if (response.offlineBuilder?.onlyLocalCall == true && (response.offlineBuilder?.callFlow == null && response.offlineBuilder?.call == null))
        throw NetFlowException("You must invoke the 'call()' functions to make only offline calls!")

    val rawLocalCall = withContext(Dispatchers.IO) {
        response.offlineBuilder?.call?.invoke() ?: response.offlineBuilder?.callFlow?.invoke()?.first()
    }

    val localCall: DisplayType? = rawLocalCall?.let { response.transform?.invoke(it) } ?: rawLocalCall as? DisplayType

    if (response.offlineBuilder?.onlyLocalCall == true) {
        localCall?.let {
            send(ResultState.Success(it))
        } ?: run {
            send(ResultState.Error(ErrorResponse(404, "Empty", ErrorResponseType.Empty)))
        }

        return@channelFlow
    }

    val shouldEmitLoading = when (localCall) {
        null -> true
        is List<*> -> localCall.isEmpty()
        else -> false
    }

    if (shouldEmitLoading) {
        send(ResultState.Loading)
    }

    immutableRequestBuilder.preCall?.invoke()

    try {
        val callResponse = response()

        response.post?.invoke()

        if (callResponse.isSuccess) {
            val apiResult = deserializeBlock(callResponse)

            apiResult?.let {
                withContext(Dispatchers.IO) { response.onNetworkSuccess?.invoke(it) }

                val displayResult: DisplayType = response.apiTransform?.invoke(it) ?: it as DisplayType

                if (response.offlineBuilder?.call == null && response.offlineBuilder?.callFlow == null) {
                    send(ResultState.Success(displayResult))
                }
            } ?: run {
                send(
                    ResultState.Error(
                        error = ErrorResponse(404, "Response null", ErrorResponseType.Empty),
                        data = localCall
                    )
                )
            }
        } else {
            send(
                ResultState.Error(
                    error = callResponse.toError(),
                    data = localCall
                )
            )
        }

        response.offlineBuilder?.call?.let { call ->
            call()?.let { result ->
                val transformed = response.transform?.invoke(result)
                send(ResultState.Success(transformed ?: result as DisplayType))
            }
        } ?: response.offlineBuilder?.callFlow?.invoke()?.collect {
            val transformed = response.transform?.invoke(it)
            send(ResultState.Success(transformed ?: it as DisplayType))
        }

    } catch (e: Exception) {
        send(
            ResultState.Error(
                error = ErrorResponse(500, e.message, ErrorResponseType.Unknown),
                data = localCall
            )
        )
    }
}

package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.extensions.toJson
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Wraps [delegate] with the chain of [interceptors]. Since `HttpEngineAdapter` is
 * the common seam across both engines and `MockNetFlowClient`, interceptors run
 * on all of them without any platform code.
 */
internal class InterceptingEngineAdapter(
    private val delegate: HttpEngineAdapter,
    private val interceptors: List<NetFlowInterceptor>,
) : HttpEngineAdapter {

    override suspend fun call(
        requestBuilder: InternalHttpRequestBuilder,
        builder: RequestBuilder
    ): NetFlowResponse {
        if (interceptors.isEmpty()) return delegate.call(requestBuilder, builder)

        val initial = initialRequest(requestBuilder, builder)

        val chain = InterceptorChain(
            interceptors = interceptors,
            index = 0,
            request = initial,
            terminal = { finalRequest ->
                requestBuilder.apply(finalRequest)
                delegate.call(requestBuilder, builder)
            },
        )

        return chain.proceed(initial)
    }

    private fun initialRequest(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder) =
        InterceptedRequest(
            method = builder.method,
            url = requestBuilder.url,
            headers = requestBuilder.headers.toList(),
            body = builder.rawBody ?: builder.body?.toJson(),
        )

    /**
     * Runs the request interceptors, then streams. A stream has no buffered response for an
     * interceptor to inspect, so interceptors must return `chain.proceed(...)`'s response unchanged
     * (or short-circuit): a 2xx short-circuit emits its body as one UTF-8 chunk, any other status
     * throws [HttpException].
     */
    override fun stream(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): Flow<ByteArray> {
        if (interceptors.isEmpty()) return delegate.stream(requestBuilder, builder)

        return flow {
            val initial = initialRequest(requestBuilder, builder)
            val upstream = arrayOfNulls<Flow<ByteArray>>(1)

            val chain = InterceptorChain(
                interceptors = interceptors,
                index = 0,
                request = initial,
                terminal = { finalRequest ->
                    requestBuilder.apply(finalRequest)
                    upstream[0] = delegate.stream(requestBuilder, builder)
                    STREAM_MARKER
                },
            )

            val result = chain.proceed(initial)
            val flowToEmit = upstream[0]
            when {
                result === STREAM_MARKER && flowToEmit != null -> emitAll(flowToEmit)
                result.isSuccess -> result.body?.let { emit(it.encodeToByteArray()) }
                else -> throw HttpException(result.code, result.errorBody)
            }
        }
    }

    private companion object {
        /** Identity token returned by the terminal so the adapter can tell "streamed" from "short-circuited". */
        val STREAM_MARKER = NetFlowResponse(code = 200, headers = emptyList(), body = null, errorBody = null)
    }
}

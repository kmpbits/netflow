package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.extensions.toJson
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse

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

        val initial = InterceptedRequest(
            method = builder.method,
            url = requestBuilder.url,
            headers = requestBuilder.headers.toList(),
            body = builder.rawBody ?: builder.body?.toJson(),
        )

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
}

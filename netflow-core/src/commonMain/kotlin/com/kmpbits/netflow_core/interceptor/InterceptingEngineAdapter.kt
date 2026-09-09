package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.extensions.toJson
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * Envolve [delegate] com a cadeia de [interceptors]. Como o `HttpEngineAdapter` é
 * o ponto comum aos dois motores e ao `MockNetFlowClient`, os interceptors correm
 * em todos sem código de plataforma.
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

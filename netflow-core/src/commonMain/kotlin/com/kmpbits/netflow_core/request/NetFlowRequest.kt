package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.platform.InternalHttpClient
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse

class NetFlowRequest internal constructor(
    @PublishedApi
    internal val builder: RequestBuilder,
    @PublishedApi
    internal val baseUrl: String,
    @PublishedApi
    internal val client: InternalHttpClient,
    @PublishedApi
    internal val requestBuilder: InternalHttpRequestBuilder
) {
    val immutableRequestBuilder = ImmutableRequestBuilder(
        preCall = builder.preCall,
        headers = builder.headers,
        builder = builder
    )

    val method: HttpMethod
        get() = builder.method

    val headers: List<Pair<HttpHeader, String>>
        get() = builder.headers

    suspend fun response(): NetFlowResponse {
        return client.call(requestBuilder, builder)
    }
}

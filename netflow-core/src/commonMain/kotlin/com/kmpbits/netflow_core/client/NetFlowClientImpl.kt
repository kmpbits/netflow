package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.RetryBuilder
import com.kmpbits.netflow_core.builders.build
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpClient
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest

internal class NetFlowClientImpl(
    private val client: InternalHttpClient,
    private val baseUrl: String,
    private val logLevel: LogLevel,
    private val retryBuilder: RetryBuilder,
    private val headers: Headers
) : NetFlowClient {

    override fun call(builder: RequestBuilder.() -> Unit): NetFlowRequest {
        return request(builder)
    }

    private fun request(builder: RequestBuilder. () -> Unit = {}): NetFlowRequest {
        val callBuilder = RequestBuilder(baseUrl, retryBuilder, headers).also(builder)
        val requestBuilder = callBuilder.build()
        val engine = object : HttpEngineAdapter {
            override suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder) =
                client.call(requestBuilder, builder)
        }
        return NetFlowRequest(callBuilder, engine, requestBuilder, logLevel)
    }
}
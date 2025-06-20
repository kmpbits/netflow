package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.build
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.logging.Logging
import com.kmpbits.netflow_core.platform.InternalHttpClient
import com.kmpbits.netflow_core.request.NetFlowRequest

internal class NetFlowClientImpl(
    private val client: InternalHttpClient,
    private val baseUrl: String,
    private val logLevel: LogLevel
) : NetFlowClient {

    override suspend fun call(builder: RequestBuilder.() -> Unit): NetFlowRequest {
        return request(builder)
    }

    private suspend fun request(builder: RequestBuilder. () -> Unit = {}): NetFlowRequest {
        val callBuilder = RequestBuilder(baseUrl).also(builder)
        val requestBuilder = callBuilder.build()

        // Log the request
        when(logLevel) {
            LogLevel.None -> {}
            LogLevel.Basic -> {}
            LogLevel.Headers -> {}
            LogLevel.Body -> Logging.logRequest(requestBuilder)
        }

        return NetFlowRequest(callBuilder, baseUrl, client, requestBuilder)
    }
}
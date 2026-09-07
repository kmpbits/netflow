package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.RetryBuilder
import com.kmpbits.netflow_core.builders.build
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpClient
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NetFlowClientImpl(
    private val client: InternalHttpClient,
    private val baseUrl: String,
    private val logLevel: LogLevel,
    private val retryBuilder: RetryBuilder,
    private val headers: Headers
) : NetFlowClient {

    private val _authState = MutableStateFlow(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun setTokens(tokens: BearerTokens) { /* wired in Task 8 */ }
    override suspend fun clearTokens() { /* wired in Task 8 */ }

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
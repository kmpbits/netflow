package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.auth.AuthConfig
import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.auth.TokenHolder
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
    private val headers: Headers,
    private val authConfig: AuthConfig? = null,
) : NetFlowClient {

    private val tokenHolder: TokenHolder? = authConfig?.let { cfg ->
        TokenHolder(
            config = cfg,
            rawClientProvider = {
                RawNetFlowClient(
                    NetFlowClientImpl(
                        client = client,
                        baseUrl = baseUrl,
                        logLevel = logLevel,
                        retryBuilder = retryBuilder,
                        headers = headers.toMutableList(),
                        authConfig = null,
                    )
                )
            },
        )
    }

    private val fallbackAuthState = MutableStateFlow(AuthState.Unknown)
    override val authState: StateFlow<AuthState> =
        tokenHolder?.authState ?: fallbackAuthState.asStateFlow()

    override suspend fun setTokens(tokens: BearerTokens) {
        tokenHolder?.set(tokens)
    }

    override suspend fun clearTokens() {
        tokenHolder?.clear()
    }

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
        return NetFlowRequest(callBuilder, engine, requestBuilder, logLevel, tokenHolder)
    }
}

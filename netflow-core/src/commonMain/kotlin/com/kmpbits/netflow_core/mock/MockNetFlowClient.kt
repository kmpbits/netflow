package com.kmpbits.netflow_core.mock

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.auth.AuthConfig
import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.auth.TokenHolder
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.client.RawNetFlowClient
import com.kmpbits.netflow_core.builders.RetryBuilder
import com.kmpbits.netflow_core.builders.build
import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.interceptor.InterceptingEngineAdapter
import com.kmpbits.netflow_core.interceptor.NetFlowInterceptor
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [NetFlowClient] implementation for testing. Intercepts all requests and
 * returns responses defined by [handler] without making any real network calls.
 *
 * Supports response delays, request recording, and assertion helpers.
 *
 * Usage:
 * ```kotlin
 * val client = MockNetFlowClient { request ->
 *     when {
 *         request.path == "todos" && request.method == HttpMethod.Get ->
 *             NetFlowMockResponse.success("""[{"id":1,"title":"Buy milk","completed":false}]""")
 *         request.path.startsWith("todos/") && request.method == HttpMethod.Delete ->
 *             NetFlowMockResponse.success(delay = 200.milliseconds)
 *         else -> NetFlowMockResponse.notFound()
 *     }
 * }
 *
 * // After test
 * client.assertCalled("todos", HttpMethod.Get)
 * client.assertCalledTimes("todos/1", HttpMethod.Delete, times = 1)
 * ```
 */
class MockNetFlowClient private constructor(
    private val auth: (AuthConfig.() -> Unit)?,
    private val interceptors: List<NetFlowInterceptor>,
    private val handler: suspend (NetFlowMockRequest) -> NetFlowMockResponse,
    sharedRecords: MutableList<NetFlowMockRequest>?,
) : NetFlowClient {

    constructor(
        auth: (AuthConfig.() -> Unit)? = null,
        interceptors: List<NetFlowInterceptor> = emptyList(),
        handler: suspend (NetFlowMockRequest) -> NetFlowMockResponse,
    ) : this(auth, interceptors, handler, null)

    private val _recordedRequests: MutableList<NetFlowMockRequest> = sharedRecords ?: mutableListOf()

    private val tokenHolder: TokenHolder? = auth?.let { block ->
        TokenHolder(
            config = AuthConfig().apply(block),
            rawClientProvider = {
                RawNetFlowClient(
                    MockNetFlowClient(
                        auth = null,
                        interceptors = interceptors,
                        handler = handler,
                        sharedRecords = _recordedRequests,
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

    /** All requests that have been made through this client, in order. */
    val recordedRequests: List<NetFlowMockRequest> get() = _recordedRequests.toList()

    /** Clears the recorded request history. */
    fun clearRecordedRequests() = _recordedRequests.clear()

    /** Throws if [path] + [method] was not called exactly [times] times. */
    fun assertCalledTimes(path: String, method: HttpMethod, times: Int) {
        val count = _recordedRequests.count { it.path == path && it.method == method }
        check(count == times) {
            "Expected $times call(s) to $method $path but recorded $count."
        }
    }

    /** Throws if [path] + [method] was never called. */
    fun assertCalled(path: String, method: HttpMethod) {
        check(_recordedRequests.any { it.path == path && it.method == method }) {
            "Expected at least one call to $method $path but none was recorded."
        }
    }

    /** Throws if [path] + [method] was called at all. */
    fun assertNotCalled(path: String, method: HttpMethod) {
        check(_recordedRequests.none { it.path == path && it.method == method }) {
            "Expected no calls to $method $path but at least one was recorded."
        }
    }

    override fun call(builder: RequestBuilder.() -> Unit): NetFlowRequest {
        val callBuilder = RequestBuilder("https://mock", RetryBuilder(), mutableListOf()).also(builder)
        val requestBuilder = callBuilder.build()

        val engine = InterceptingEngineAdapter(
            delegate = object : HttpEngineAdapter {
                override suspend fun call(
                    requestBuilder: InternalHttpRequestBuilder,
                    builder: RequestBuilder
                ): NetFlowResponse {
                    // Headers come from requestBuilder (InternalHttpRequestBuilder), not
                    // builder.headers: a NetFlowInterceptor writes its changes there via
                    // apply(), never onto the RequestBuilder's mutable list.
                    val mockRequest = NetFlowMockRequest(
                        path = builder.path,
                        method = builder.method,
                        body = builder.body,
                        rawBody = builder.rawBody,
                        headers = requestBuilder.headers.map { it.first.header to it.second },
                        parts = builder.parts.toList().ifEmpty { null },
                        parameters = builder.parameters.toList(),
                        onProgress = builder.onProgress,
                    )
                    _recordedRequests.add(mockRequest)

                    val mockResponse = handler(mockRequest)

                    if (mockResponse.delay.inWholeMilliseconds > 0) {
                        delay(mockResponse.delay)
                    }

                    return NetFlowResponse(
                        code = mockResponse.code,
                        headers = mockResponse.headers.map { (name, value) ->
                            Header(HttpHeader.custom(name), value)
                        },
                        body = mockResponse.body,
                        errorBody = mockResponse.errorBody
                    )
                }
            },
            interceptors = interceptors,
        )

        return NetFlowRequest(callBuilder, engine, requestBuilder, LogLevel.None, tokenHolder)
    }
}

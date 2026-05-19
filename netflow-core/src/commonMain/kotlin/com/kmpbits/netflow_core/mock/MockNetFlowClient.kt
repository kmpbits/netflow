package com.kmpbits.netflow_core.mock

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.RetryBuilder
import com.kmpbits.netflow_core.builders.build
import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.delay

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
class MockNetFlowClient(
    private val handler: suspend (NetFlowMockRequest) -> NetFlowMockResponse
) : NetFlowClient {

    private val _recordedRequests = mutableListOf<NetFlowMockRequest>()

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

        val engine = object : HttpEngineAdapter {
            override suspend fun call(
                requestBuilder: InternalHttpRequestBuilder,
                builder: RequestBuilder
            ): NetFlowResponse {
                val mockRequest = NetFlowMockRequest(
                    path = builder.path,
                    method = builder.method,
                    body = builder.body,
                    headers = builder.headers.map { it.first.header to it.second }
                )
                _recordedRequests.add(mockRequest)

                val mockResponse = handler(mockRequest)

                if (mockResponse.delay.inWholeMilliseconds > 0) {
                    delay(mockResponse.delay)
                }

                return NetFlowResponse(
                    code = mockResponse.code,
                    headers = builder.headers,
                    body = mockResponse.body,
                    errorBody = mockResponse.errorBody
                )
            }
        }

        return NetFlowRequest(callBuilder, engine, requestBuilder, LogLevel.None)
    }
}

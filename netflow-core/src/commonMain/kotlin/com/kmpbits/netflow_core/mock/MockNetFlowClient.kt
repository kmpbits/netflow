package com.kmpbits.netflow_core.mock

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.RetryBuilder
import com.kmpbits.netflow_core.builders.build
import com.kmpbits.netflow_core.client.NetFlowClient
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest
import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * A [NetFlowClient] implementation for testing. Intercepts all requests and
 * returns responses defined by [handler] without making any real network calls.
 *
 * Usage:
 * ```kotlin
 * val client = MockNetFlowClient { request ->
 *     when {
 *         request.path == "todos" && request.method == HttpMethod.Get ->
 *             NetFlowMockResponse.success("""[{"id":1,"title":"Buy milk","completed":false}]""")
 *         request.path.startsWith("todos/") && request.method == HttpMethod.Delete ->
 *             NetFlowMockResponse.success()
 *         else -> NetFlowMockResponse.notFound()
 *     }
 * }
 * ```
 */
class MockNetFlowClient(
    private val handler: (NetFlowMockRequest) -> NetFlowMockResponse
) : NetFlowClient {

    override fun call(builder: RequestBuilder.() -> Unit): NetFlowRequest {
        val callBuilder = RequestBuilder("", RetryBuilder(), mutableListOf()).also(builder)
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
                val mockResponse = handler(mockRequest)
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

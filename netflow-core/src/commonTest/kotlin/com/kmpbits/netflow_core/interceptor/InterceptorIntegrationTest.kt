package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.RetryTimes
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterceptorIntegrationTest {

    @Test
    fun `um header acrescentado por interceptor chega ao pedido registado`() = runTest {
        val client = MockNetFlowClient(
            interceptors = listOf(
                NetFlowInterceptor { chain ->
                    chain.proceed(
                        chain.request.newBuilder()
                            .header(Header(HttpHeader.custom("X-Trace-Id"), "abc"))
                            .build()
                    )
                }
            ),
        ) { NetFlowMockResponse.success(body = "ok") }

        client.call { path = "todos" }.response()

        val recorded = client.recordedRequests.single()
        assertTrue(recorded.headers.any { it.first == "X-Trace-Id" && it.second == "abc" })
    }

    @Test
    fun `um interceptor em curto-circuito impede o registo do pedido`() = runTest {
        val client = MockNetFlowClient(
            interceptors = listOf(
                NetFlowInterceptor { NetFlowResponse(200, emptyList(), "cached", null) }
            ),
        ) { NetFlowMockResponse.success(body = "da rede") }

        val response = client.call { path = "todos" }.response()

        assertEquals("cached", response.body)
        assertTrue(client.recordedRequests.isEmpty())
    }

    @Test
    fun `o interceptor corre uma vez por tentativa de retry`() = runTest {
        var runs = 0
        val client = MockNetFlowClient(
            interceptors = listOf(
                NetFlowInterceptor { chain -> runs++; chain.proceed(chain.request) }
            ),
        ) { throw IllegalStateException("boom") }

        client.call {
            path = "todos"
            retry { times = RetryTimes.THREE }
        }.response()

        assertEquals(3, runs)
    }
}

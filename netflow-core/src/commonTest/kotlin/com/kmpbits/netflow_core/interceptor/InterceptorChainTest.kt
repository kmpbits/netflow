package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InterceptorChainTest {

    private val initial = InterceptedRequest(
        method = HttpMethod.Get,
        url = "https://example.com/todos",
        headers = emptyList(),
        body = null,
    )

    private fun ok(body: String) = NetFlowResponse(200, emptyList(), body, null)

    private suspend fun run(
        interceptors: List<NetFlowInterceptor>,
        terminal: suspend (InterceptedRequest) -> NetFlowResponse,
    ): NetFlowResponse =
        InterceptorChain(interceptors, 0, initial, terminal).proceed(initial)

    @Test
    fun `with no interceptors it reaches the terminal with the original request`() = runTest {
        var seen: InterceptedRequest? = null
        val response = run(emptyList()) { seen = it; ok("done") }

        assertEquals("https://example.com/todos", seen?.url)
        assertEquals("done", response.body)
    }

    @Test
    fun `an interceptor can add a header that reaches the terminal`() = runTest {
        val adder = NetFlowInterceptor { chain ->
            chain.proceed(
                chain.request.newBuilder()
                    .header(Header(HttpHeader.custom("X-Trace-Id"), "abc"))
                    .build()
            )
        }

        var seen: InterceptedRequest? = null
        run(listOf(adder)) { seen = it; ok("done") }

        assertEquals("abc", seen?.headers?.single()?.second)
    }

    @Test
    fun `interceptors run in registration order`() = runTest {
        val order = mutableListOf<String>()
        val first = NetFlowInterceptor { chain -> order.add("first-in"); val r = chain.proceed(chain.request); order.add("first-out"); r }
        val second = NetFlowInterceptor { chain -> order.add("second-in"); val r = chain.proceed(chain.request); order.add("second-out"); r }

        run(listOf(first, second)) { order.add("terminal"); ok("done") }

        assertEquals(listOf("first-in", "second-in", "terminal", "second-out", "first-out"), order)
    }

    @Test
    fun `an interceptor can short-circuit without reaching the terminal`() = runTest {
        var terminalCalled = false
        val shortCircuit = NetFlowInterceptor { NetFlowResponse(418, emptyList(), "cached", null) }

        val response = run(listOf(shortCircuit)) { terminalCalled = true; ok("done") }

        assertEquals(418, response.code)
        assertEquals("cached", response.body)
        assertTrue(!terminalCalled)
    }

    @Test
    fun `an interceptor can observe the response coming back`() = runTest {
        var observed: Int? = null
        val observer = NetFlowInterceptor { chain ->
            val r = chain.proceed(chain.request)
            observed = r.code
            r
        }

        run(listOf(observer)) { NetFlowResponse(503, emptyList(), null, "unavailable") }

        assertEquals(503, observed)
    }
}

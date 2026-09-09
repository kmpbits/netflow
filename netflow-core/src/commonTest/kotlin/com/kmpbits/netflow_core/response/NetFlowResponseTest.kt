package com.kmpbits.netflow_core.response

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetFlowResponseTest {

    @Test
    fun `can be constructed from consumer code`() {
        val response = NetFlowResponse(
            code = 200,
            headers = listOf(Header(HttpHeader.CACHE_CONTROL, "no-store")),
            body = "ok",
            errorBody = null,
        )

        assertTrue(response.isSuccess)
        assertEquals("no-store", response.headers.single().second)
    }

    @Test
    fun `the mock returns the response headers and not the request headers`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(body = "ok", headers = mapOf("X-Request-Id" to "42")) }

        val response = client.call { path = "todos" }.response()

        assertEquals("42", response.headers.single { it.first.header == "X-Request-Id" }.second)
    }
}

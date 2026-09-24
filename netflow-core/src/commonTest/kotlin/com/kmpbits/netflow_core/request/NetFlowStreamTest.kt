package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.interceptor.NetFlowInterceptor
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NetFlowStreamTest {

    private val chunks = listOf(byteArrayOf(1, 2), byteArrayOf(3))

    @Test
    fun `stream emits the mock chunks in order`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.stream(chunks) }

        val out = client.call { path = "files"; method = HttpMethod.Get }.stream().toList()

        assertEquals(2, out.size)
        assertContentEquals(byteArrayOf(1, 2), out[0])
        assertContentEquals(byteArrayOf(3), out[1])
        client.assertCalledTimes("files", HttpMethod.Get, times = 1)
    }

    @Test
    fun `stream is cold - nothing is recorded until collected`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.stream(chunks) }

        val flow = client.call { path = "files" }.stream()

        assertTrue(client.recordedRequests.isEmpty())
        flow.toList()
        assertEquals(1, client.recordedRequests.size)
    }

    @Test
    fun `a string body is emitted as one utf-8 chunk`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("héllo") }

        val out = client.call { path = "files" }.stream().toList()

        assertEquals(1, out.size)
        assertEquals("héllo", out[0].decodeToString())
    }

    @Test
    fun `non-2xx throws HttpException with the error body`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.error(code = 404, errorBody = "nope") }

        val e = assertFailsWith<HttpException> { client.call { path = "files" }.stream().toList() }

        assertEquals(404, e.code)
        assertEquals("nope", e.message)
    }

    @Test
    fun `request interceptors run and their headers reach the request`() = runTest {
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
        ) { NetFlowMockResponse.stream(chunks) }

        val out = client.call { path = "files" }.stream().toList()

        assertEquals(2, out.size)
        assertTrue(client.recordedRequests.single().headers.any { it.first == "X-Trace-Id" && it.second == "abc" })
    }

    @Test
    fun `a short-circuiting interceptor with 2xx emits its body as one chunk without hitting the network`() = runTest {
        val client = MockNetFlowClient(
            interceptors = listOf(NetFlowInterceptor { NetFlowResponse(200, emptyList(), "cached", null) }),
        ) { NetFlowMockResponse.stream(chunks) }

        val out = client.call { path = "files" }.stream().toList()

        assertEquals(listOf("cached"), out.map { it.decodeToString() })
        assertTrue(client.recordedRequests.isEmpty())
    }

    @Test
    fun `a short-circuiting interceptor with non-2xx throws HttpException`() = runTest {
        val client = MockNetFlowClient(
            interceptors = listOf(NetFlowInterceptor { NetFlowResponse(503, emptyList(), null, "down") }),
        ) { NetFlowMockResponse.stream(chunks) }

        val e = assertFailsWith<HttpException> { client.call { path = "files" }.stream().toList() }

        assertEquals(503, e.code)
        assertEquals("down", e.message)
    }

    @Test
    fun `auth attaches the bearer token to the stream request`() = runTest {
        var seenAuth: String? = null
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { error("should not refresh") }
        }) { req ->
            seenAuth = req["Authorization"]
            NetFlowMockResponse.stream(chunks)
        }

        client.call { path = "files" }.stream().toList()

        assertEquals("Bearer good", seenAuth)
    }

    @Test
    fun `a 401 before the first byte refreshes the token and retries once`() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { BearerTokens("fresh", "r2") }
        }) { req ->
            when (req["Authorization"]) {
                "Bearer expired" -> NetFlowMockResponse.unauthorized()
                "Bearer fresh" -> NetFlowMockResponse.stream(chunks)
                else -> NetFlowMockResponse.notFound()
            }
        }

        val out = client.call { path = "files" }.stream().toList()

        assertEquals(2, out.size)
        client.assertCalledTimes("files", HttpMethod.Get, times = 2)
    }

    @Test
    fun `a 401 with a failed refresh surfaces the HttpException`() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { null }
        }) { NetFlowMockResponse.unauthorized() }

        val e = assertFailsWith<HttpException> { client.call { path = "files" }.stream().toList() }

        assertEquals(401, e.code)
    }

    @Test
    fun `skipAuth streams do not get an Authorization header`() = runTest {
        var seenAuth: String? = "unset"
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { null }
        }) { req ->
            seenAuth = req["Authorization"]
            NetFlowMockResponse.stream(chunks)
        }

        client.call { path = "files"; skipAuth() }.stream().toList()

        assertEquals(null, seenAuth)
    }

    @Test
    fun `onDownloadProgress receives the cumulative bytes and the content length`() = runTest {
        val seen = mutableListOf<Pair<Long, Long>>()
        val client = MockNetFlowClient {
            NetFlowMockResponse.stream(chunks, headers = mapOf("Content-Length" to "3"))
        }

        client.call { path = "files"; onDownloadProgress { r, t -> seen += r to t } }.stream().toList()

        assertEquals(listOf(2L to 3L, 3L to 3L), seen)
    }

    @Test
    fun `onDownloadProgress reports minus one when the mock has no content length`() = runTest {
        val seen = mutableListOf<Pair<Long, Long>>()
        val client = MockNetFlowClient { NetFlowMockResponse.stream(chunks) }

        client.call { path = "files"; onDownloadProgress { r, t -> seen += r to t } }.stream().toList()

        assertEquals(listOf(2L to -1L, 3L to -1L), seen)
    }

    @Test
    fun `the callback is recorded on the mock request`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.stream(chunks) }

        client.call { path = "files"; onDownloadProgress { _, _ -> } }.stream().toList()
        client.call { path = "files" }.stream().toList()

        assertTrue(client.recordedRequests[0].onDownloadProgress != null)
        assertEquals(null, client.recordedRequests[1].onDownloadProgress)
    }
}

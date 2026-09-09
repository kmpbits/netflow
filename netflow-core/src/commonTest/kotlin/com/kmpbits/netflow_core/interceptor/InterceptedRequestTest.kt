package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InterceptedRequestTest {

    private fun request() = InterceptedRequest(
        method = HttpMethod.Get,
        url = "https://example.com/todos",
        headers = listOf(Header(HttpHeader.ACCEPT, "application/json")),
        body = null,
    )

    @Test
    fun `header adds a new header`() {
        val result = request().newBuilder()
            .header(Header(HttpHeader.custom("X-Trace-Id"), "abc"))
            .build()

        assertEquals("abc", result.headers.first { it.first.header == "X-Trace-Id" }.second)
        assertEquals(2, result.headers.size)
    }

    @Test
    fun `header replaces the existing value for the same name`() {
        val result = request().newBuilder()
            .header(Header(HttpHeader.ACCEPT, "text/plain"))
            .build()

        assertEquals(1, result.headers.size)
        assertEquals("text/plain", result.headers.single().second)
    }

    @Test
    fun `header replaces ignoring case in the name`() {
        val result = request().newBuilder()
            .header(Header(HttpHeader.custom("accept"), "text/plain"))
            .build()

        assertEquals(1, result.headers.size)
        assertEquals("text/plain", result.headers.single().second)
    }

    @Test
    fun `removeHeader drops the header ignoring case`() {
        val result = request().newBuilder()
            .removeHeader(HttpHeader.custom("ACCEPT"))
            .build()

        assertTrue(result.headers.isEmpty())
    }

    @Test
    fun `url replaces the url while keeping the rest`() {
        val result = request().newBuilder()
            .url("https://other.com/x")
            .build()

        assertEquals("https://other.com/x", result.url)
        assertEquals(HttpMethod.Get, result.method)
        assertEquals(1, result.headers.size)
    }

    @Test
    fun `the body passes through the builder unchanged`() {
        val original = InterceptedRequest(
            method = HttpMethod.Post,
            url = "https://example.com/todos",
            headers = emptyList(),
            body = "{\"a\":1}",
        )

        assertEquals("{\"a\":1}", original.newBuilder().build().body)
    }

    @Test
    fun `the original request is not mutated by the builder`() {
        val original = request()
        original.newBuilder().removeHeader(HttpHeader.ACCEPT).build()

        assertEquals(1, original.headers.size)
        assertNull(original.body)
    }
}

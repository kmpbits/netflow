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
    fun `header adiciona um header novo`() {
        val result = request().newBuilder()
            .header(Header(HttpHeader.custom("X-Trace-Id"), "abc"))
            .build()

        assertEquals("abc", result.headers.first { it.first.header == "X-Trace-Id" }.second)
        assertEquals(2, result.headers.size)
    }

    @Test
    fun `header substitui o valor existente do mesmo nome`() {
        val result = request().newBuilder()
            .header(Header(HttpHeader.ACCEPT, "text/plain"))
            .build()

        assertEquals(1, result.headers.size)
        assertEquals("text/plain", result.headers.single().second)
    }

    @Test
    fun `header substitui ignorando maiusculas no nome`() {
        val result = request().newBuilder()
            .header(Header(HttpHeader.custom("accept"), "text/plain"))
            .build()

        assertEquals(1, result.headers.size)
        assertEquals("text/plain", result.headers.single().second)
    }

    @Test
    fun `removeHeader tira o header ignorando maiusculas`() {
        val result = request().newBuilder()
            .removeHeader(HttpHeader.custom("ACCEPT"))
            .build()

        assertTrue(result.headers.isEmpty())
    }

    @Test
    fun `url substitui o url mantendo o resto`() {
        val result = request().newBuilder()
            .url("https://outro.com/x")
            .build()

        assertEquals("https://outro.com/x", result.url)
        assertEquals(HttpMethod.Get, result.method)
        assertEquals(1, result.headers.size)
    }

    @Test
    fun `o body atravessa o builder sem alteracao`() {
        val original = InterceptedRequest(
            method = HttpMethod.Post,
            url = "https://example.com/todos",
            headers = emptyList(),
            body = "{\"a\":1}",
        )

        assertEquals("{\"a\":1}", original.newBuilder().build().body)
    }

    @Test
    fun `o pedido original nao e alterado pelo builder`() {
        val original = request()
        original.newBuilder().removeHeader(HttpHeader.ACCEPT).build()

        assertEquals(1, original.headers.size)
        assertNull(original.body)
    }
}

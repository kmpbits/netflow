package com.kmpbits.netflow_core.builders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientBuilderPinningTest {

    private val hashA = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private val hashB = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBA="

    @Test
    fun `sem bloco pinning o cliente nao tem pins`() {
        val builder = ClientBuilder().apply { baseUrl = "https://example.com" }
        val client = builder.okHttpClientForTest()

        assertTrue(client.certificatePinner.pins.isEmpty())
    }

    @Test
    fun `o bloco pinning chega ao CertificatePinner`() {
        val builder = ClientBuilder().apply {
            baseUrl = "https://example.com"
            pinning { pin("api.exemplo.com", hashA, hashB) }
        }
        val client = builder.okHttpClientForTest()

        assertEquals(2, client.certificatePinner.pins.size)
        assertTrue(client.certificatePinner.pins.all { it.pattern == "api.exemplo.com" })
    }

    @Test
    fun `um padrao com wildcard chega intacto ao CertificatePinner`() {
        val builder = ClientBuilder().apply {
            baseUrl = "https://example.com"
            pinning { pin("*.exemplo.com", hashA) }
        }
        val client = builder.okHttpClientForTest()

        assertEquals("*.exemplo.com", client.certificatePinner.pins.single().pattern)
    }

    @Test
    fun `followRedirects e falso por omissao`() {
        val builder = ClientBuilder().apply { baseUrl = "https://example.com" }

        assertFalse(builder.okHttpClientForTest().followRedirects)
    }

    @Test
    fun `followRedirects chega ao OkHttpClient quando ligado`() {
        val builder = ClientBuilder().apply {
            baseUrl = "https://example.com"
            followRedirects = true
        }

        assertTrue(builder.okHttpClientForTest().followRedirects)
    }
}

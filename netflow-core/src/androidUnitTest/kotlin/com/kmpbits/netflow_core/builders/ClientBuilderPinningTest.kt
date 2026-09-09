package com.kmpbits.netflow_core.builders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientBuilderPinningTest {

    private val hashA = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private val hashB = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBA="

    @Test
    fun `without a pinning block the client has no pins`() {
        val builder = ClientBuilder().apply { baseUrl = "https://example.com" }
        val client = builder.okHttpClientForTest()

        assertTrue(client.certificatePinner.pins.isEmpty())
    }

    @Test
    fun `the pinning block reaches the CertificatePinner`() {
        val builder = ClientBuilder().apply {
            baseUrl = "https://example.com"
            pinning { pin("api.example.com", hashA, hashB) }
        }
        val client = builder.okHttpClientForTest()

        assertEquals(2, client.certificatePinner.pins.size)
        assertTrue(client.certificatePinner.pins.all { it.pattern == "api.example.com" })
    }

    @Test
    fun `a wildcard pattern reaches the CertificatePinner unchanged`() {
        val builder = ClientBuilder().apply {
            baseUrl = "https://example.com"
            pinning { pin("*.example.com", hashA) }
        }
        val client = builder.okHttpClientForTest()

        assertEquals("*.example.com", client.certificatePinner.pins.single().pattern)
    }

    @Test
    fun `followRedirects is false by default`() {
        val builder = ClientBuilder().apply { baseUrl = "https://example.com" }

        assertFalse(builder.okHttpClientForTest().followRedirects)
    }

    @Test
    fun `followRedirects reaches the OkHttpClient when enabled`() {
        val builder = ClientBuilder().apply {
            baseUrl = "https://example.com"
            followRedirects = true
        }

        assertTrue(builder.okHttpClientForTest().followRedirects)
    }
}

package com.kmpbits.netflow_core.pinning

import com.kmpbits.netflow_core.exceptions.NetFlowException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PinningConfigTest {

    // A 32-byte all-zero SHA-256, base64-encoded — valid format.
    private val validHash = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    @Test
    fun `accepts a well-formed pin`() {
        val config = PinningConfig().apply { pin("api.example.com", validHash) }
        config.validate()

        assertEquals(1, config.pins.size)
        assertEquals("api.example.com", config.pins.single().pattern)
    }

    @Test
    fun `accepts several pins for the same host`() {
        val config = PinningConfig().apply { pin("api.example.com", validHash, validHash) }
        config.validate()

        assertEquals(2, config.pins.single().hashes.size)
    }

    @Test
    fun `rejects a hash without the sha256 prefix`() {
        val config = PinningConfig().apply { pin("api.example.com", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejects invalid base64`() {
        val config = PinningConfig().apply { pin("api.example.com", "sha256/not-base64!!!") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejects a hash whose length is not 32 bytes`() {
        val config = PinningConfig().apply { pin("api.example.com", "sha256/AAAA") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejects a blank host`() {
        val config = PinningConfig().apply { pin("  ", validHash) }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejects a host with no pin at all`() {
        val config = PinningConfig().apply { pin("api.example.com") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `hashesFor merges the pins from every matching pattern`() {
        val other = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBA="
        val config = PinningConfig().apply {
            pin("api.example.com", validHash)
            pin("*.example.com", other)
        }

        assertEquals(listOf(validHash, other), config.hashesFor("api.example.com"))
        assertEquals(emptyList(), config.hashesFor("other.com"))
    }

    @Test
    fun `netflowClient blows up at construction with an invalid pin`() {
        assertFailsWith<NetFlowException> {
            com.kmpbits.netflow_core.extensions.netflowClient {
                baseUrl = "https://example.com"
                pinning { pin("api.example.com", "sha256/xx") }
            }
        }
    }
}

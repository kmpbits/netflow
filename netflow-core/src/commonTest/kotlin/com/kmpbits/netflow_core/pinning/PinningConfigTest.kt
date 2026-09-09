package com.kmpbits.netflow_core.pinning

import com.kmpbits.netflow_core.exceptions.NetFlowException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PinningConfigTest {

    // SHA-256 de 32 bytes a zero, em base64 — formato válido.
    private val validHash = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    @Test
    fun `aceita um pin bem formado`() {
        val config = PinningConfig().apply { pin("api.exemplo.com", validHash) }
        config.validate()

        assertEquals(1, config.pins.size)
        assertEquals("api.exemplo.com", config.pins.single().pattern)
    }

    @Test
    fun `aceita varios pins para o mesmo host`() {
        val config = PinningConfig().apply { pin("api.exemplo.com", validHash, validHash) }
        config.validate()

        assertEquals(2, config.pins.single().hashes.size)
    }

    @Test
    fun `rejeita hash sem o prefixo sha256`() {
        val config = PinningConfig().apply { pin("api.exemplo.com", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejeita base64 invalido`() {
        val config = PinningConfig().apply { pin("api.exemplo.com", "sha256/nao-e-base64!!!") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejeita hash com comprimento diferente de 32 bytes`() {
        val config = PinningConfig().apply { pin("api.exemplo.com", "sha256/AAAA") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejeita host vazio`() {
        val config = PinningConfig().apply { pin("  ", validHash) }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `rejeita um host sem nenhum pin`() {
        val config = PinningConfig().apply { pin("api.exemplo.com") }

        assertFailsWith<NetFlowException> { config.validate() }
    }

    @Test
    fun `hashesFor junta os pins de todos os padroes que casam`() {
        val other = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBA="
        val config = PinningConfig().apply {
            pin("api.exemplo.com", validHash)
            pin("*.exemplo.com", other)
        }

        assertEquals(listOf(validHash, other), config.hashesFor("api.exemplo.com"))
        assertEquals(emptyList(), config.hashesFor("outro.com"))
    }

    @Test
    fun `netflowClient rebenta na construcao com um pin invalido`() {
        assertFailsWith<NetFlowException> {
            com.kmpbits.netflow_core.extensions.netflowClient {
                baseUrl = "https://example.com"
                pinning { pin("api.exemplo.com", "sha256/xx") }
            }
        }
    }
}

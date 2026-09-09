package com.kmpbits.netflow_core.pinning

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostMatcherTest {

    @Test
    fun `padrao exacto casa apenas com o proprio host`() {
        assertTrue(matchesPattern("api.exemplo.com", "api.exemplo.com"))
        assertFalse(matchesPattern("api.exemplo.com", "www.api.exemplo.com"))
        assertFalse(matchesPattern("api.exemplo.com", "exemplo.com"))
    }

    @Test
    fun `padrao exacto ignora maiusculas`() {
        assertTrue(matchesPattern("API.Exemplo.COM", "api.exemplo.com"))
    }

    @Test
    fun `wildcard casa exactamente um nivel de subdominio`() {
        assertTrue(matchesPattern("*.exemplo.com", "api.exemplo.com"))
        assertFalse(matchesPattern("*.exemplo.com", "a.b.exemplo.com"))
    }

    @Test
    fun `wildcard nao casa com o dominio base`() {
        assertFalse(matchesPattern("*.exemplo.com", "exemplo.com"))
    }

    @Test
    fun `wildcard nao casa com outro dominio`() {
        assertFalse(matchesPattern("*.exemplo.com", "api.outro.com"))
    }
}

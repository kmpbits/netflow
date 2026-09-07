package com.kmpbits.netflow_core.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BearerTokensTest {

    @Test
    fun `refreshToken defaults to null`() {
        val tokens = BearerTokens(accessToken = "abc")
        assertEquals("abc", tokens.accessToken)
        assertNull(tokens.refreshToken)
    }

    @Test
    fun `holds both tokens`() {
        val tokens = BearerTokens(accessToken = "abc", refreshToken = "xyz")
        assertEquals("xyz", tokens.refreshToken)
    }

    @Test
    fun `AuthState has three states`() {
        assertEquals(3, AuthState.entries.size)
    }
}

package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.extensions.netflowClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClientBuilderAuthTest {

    @Test
    fun `authConfig is null by default`() {
        val builder = ClientBuilder()
        assertNull(builder.authConfig)
    }

    @Test
    fun `auth block populates authConfig with scheme`() {
        val builder = ClientBuilder().apply {
            auth {
                scheme = "Token"
                loadTokens { null }
            }
        }
        assertEquals("Token", builder.authConfig?.scheme)
    }

    @Test
    fun `netflowClient builds successfully with an auth block`() {
        val client = netflowClient {
            baseUrl = "https://example.com"
            auth {
                loadTokens { null }
                refreshTokens { null }
            }
        }
        assertEquals(AuthState.Unknown, client.authState.value)
    }
}

package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.client.RawNetFlowClient
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthConfigTest {

    @Test
    fun `scheme defaults to Bearer`() {
        val config = AuthConfig()
        assertEquals("Bearer", config.scheme)
    }

    @Test
    fun `blocks are null until set`() {
        val config = AuthConfig()
        assertNull(config.loadTokensBlock)
        assertNull(config.refreshTokensBlock)
    }

    @Test
    fun `loadTokens block is captured and invocable`() = runTest {
        val config = AuthConfig().apply {
            loadTokens { BearerTokens("seed", "r") }
        }
        assertEquals(BearerTokens("seed", "r"), config.loadTokensBlock!!.invoke())
    }

    @Test
    fun `refreshTokens block receives RefreshScope and returns tokens`() = runTest {
        val config = AuthConfig().apply {
            refreshTokens { BearerTokens("new-$accessToken", refreshToken) }
        }
        val scope = RefreshScope(accessToken = "old", refreshToken = "r1")
        val raw = RawNetFlowClient(MockNetFlowClient { NetFlowMockResponse.success() })
        val result = config.refreshTokensBlock!!.invoke(scope, raw)
        assertEquals(BearerTokens("new-old", "r1"), result)
    }

    @Test
    fun `AuthConfig constructor is usable for tests`() {
        assertNotNull(AuthConfig())
    }
}

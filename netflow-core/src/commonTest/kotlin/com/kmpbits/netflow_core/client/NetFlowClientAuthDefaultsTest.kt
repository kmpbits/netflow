package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetFlowClientAuthDefaultsTest {

    @Test
    fun `authState defaults to Unknown when no auth configured`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success() }
        assertEquals(AuthState.Unknown, client.authState.value)
    }

    @Test
    fun `setTokens and clearTokens are inert no-ops when no auth configured`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success() }
        client.setTokens(BearerTokens("abc"))
        client.clearTokens()
        assertEquals(AuthState.Unknown, client.authState.value)
    }
}

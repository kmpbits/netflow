package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.client.prepareCall
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthPrepareCallTest {

    @Test
    fun `prepareCall inherits the 401 refresh-and-retry`() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { BearerTokens("fresh", "r2") }
        }) { req ->
            if (req["Authorization"] == "Bearer fresh") NetFlowMockResponse.success("""{"page":1}""")
            else NetFlowMockResponse.unauthorized()
        }

        val res = client.prepareCall { path = "items" }.response()

        assertEquals(200, res.code)
        client.assertCalledTimes("items", HttpMethod.Get, times = 2)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }
}

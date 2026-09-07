package com.kmpbits.sample

import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.auth.InMemoryTokenStorage
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.sample.android.core.di.sampleAuthConfig
import com.kmpbits.sample.android.data.remote.createTodoApi
import com.kmpbits.netflow_core.states.AsyncState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the exact `auth { }` config used by `authNetworkModule` (via the shared
 * [sampleAuthConfig]) against a mock backend — no network, real wiring.
 */
class AuthClientSetupTest {

    @Test
    fun refreshes_via_the_generated_AuthApi_and_retries_the_original_request() = runTest {
        val storage = InMemoryTokenStorage(BearerTokens("expired-access", "refresh-1"))

        val client = MockNetFlowClient(auth = sampleAuthConfig(storage)) { req ->
            when {
                req.path == "auth/refresh" -> NetFlowMockResponse.success(
                    """{"access_token":"fresh-access","refresh_token":"refresh-2"}"""
                )
                req["Authorization"] == "Bearer expired-access" -> NetFlowMockResponse.unauthorized()
                req["Authorization"] == "Bearer fresh-access" ->
                    NetFlowMockResponse.success("""[{"userId":1,"id":1,"title":"a","completed":false}]""")
                else -> NetFlowMockResponse.notFound()
            }
        }

        val state = client.createTodoApi().getTodos(completed = null)

        assertTrue(state is AsyncState.Success, "expected Success, was $state")
        client.assertCalledTimes("todos", HttpMethod.Get, times = 2)   // 401 then retry
        client.assertCalledTimes("auth/refresh", HttpMethod.Post, times = 1)
        assertEquals(AuthState.Authenticated, client.authState.value)
        assertEquals(BearerTokens("fresh-access", "refresh-2"), storage.load())   // persisted
    }
}

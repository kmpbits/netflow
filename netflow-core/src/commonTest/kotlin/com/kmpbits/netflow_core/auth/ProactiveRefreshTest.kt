package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ProactiveRefreshTest {

    private fun now() = currentEpochSeconds()

    @Test
    fun `refreshes before sending when the JWT is within the leeway window`() = runTest {
        var refreshCalls = 0
        val client = MockNetFlowClient(auth = {
            refreshLeeway = 60.seconds
            loadTokens { BearerTokens(jwtExpiringAt(now() + 10), "r1") }   // expires in 10s
            refreshTokens { refreshCalls++; BearerTokens(jwtExpiringAt(now() + 3600), "r2") }
        }) { req ->
            // the stale token must never reach the handler
            assertEquals(false, req["Authorization"]!!.contains(jwtExpiringAt(now() + 10)))
            NetFlowMockResponse.success("""{"ok":1}""")
        }

        val res = client.call { path = "me" }.response()

        assertEquals(200, res.code)
        assertEquals(1, refreshCalls)
        client.assertCalledTimes("me", HttpMethod.Get, times = 1)   // no 401 round-trip
        assertEquals(AuthState.Authenticated, client.authState.value)
    }

    @Test
    fun `does not refresh when the JWT is still comfortably valid`() = runTest {
        var refreshCalls = 0
        val client = MockNetFlowClient(auth = {
            refreshLeeway = 60.seconds
            loadTokens { BearerTokens(jwtExpiringAt(now() + 3600), "r1") }
            refreshTokens { refreshCalls++; BearerTokens("x", "y") }
        }) { NetFlowMockResponse.success("""{"ok":1}""") }

        client.call { path = "me" }.response()

        assertEquals(0, refreshCalls)
    }

    @Test
    fun `disabled by default - expired JWT still goes out and relies on the 401 path`() = runTest {
        var refreshCalls = 0
        val expired = jwtExpiringAt(now() - 10)
        val client = MockNetFlowClient(auth = {
            // no refreshLeeway
            loadTokens { BearerTokens(expired, "r1") }
            refreshTokens { refreshCalls++; BearerTokens(jwtExpiringAt(now() + 3600), "r2") }
        }) { req ->
            if (req["Authorization"] == "Bearer $expired") NetFlowMockResponse.unauthorized()
            else NetFlowMockResponse.success("""{"ok":1}""")
        }

        val res = client.call { path = "me" }.response()

        assertEquals(200, res.code)
        assertEquals(1, refreshCalls)
        client.assertCalledTimes("me", HttpMethod.Get, times = 2)   // reactive: 401 then retry
    }

    @Test
    fun `opaque token with leeway set silently falls back to the reactive path`() = runTest {
        var refreshCalls = 0
        val client = MockNetFlowClient(auth = {
            refreshLeeway = 60.seconds
            loadTokens { BearerTokens("opaque-token", "r1") }
            refreshTokens { refreshCalls++; BearerTokens("opaque-fresh", "r2") }
        }) { req ->
            if (req["Authorization"] == "Bearer opaque-token") NetFlowMockResponse.unauthorized()
            else NetFlowMockResponse.success("""{"ok":1}""")
        }

        val res = client.call { path = "me" }.response()

        assertEquals(200, res.code)
        assertEquals(1, refreshCalls)
        client.assertCalledTimes("me", HttpMethod.Get, times = 2)
    }

    @Test
    fun `concurrent requests on a stale JWT trigger a single proactive refresh`() = runTest {
        var refreshCalls = 0
        val client = MockNetFlowClient(auth = {
            refreshLeeway = 60.seconds
            loadTokens { BearerTokens(jwtExpiringAt(now() + 5), "r1") }
            refreshTokens { refreshCalls++; BearerTokens(jwtExpiringAt(now() + 3600), "r2") }
        }) { NetFlowMockResponse.success("""{"ok":1}""") }

        val results = List(5) {
            async { client.call { path = "me" }.response() }
        }.awaitAll()

        results.forEach { assertEquals(200, it.code) }
        assertEquals(1, refreshCalls)
        client.assertCalledTimes("me", HttpMethod.Get, times = 5)
    }
}

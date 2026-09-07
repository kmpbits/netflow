package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthIntegrationTest {

    @Test
    fun `valid token attaches Authorization header and succeeds without refresh`() = runTest {
        var seenAuth: String? = null
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { error("should not refresh") }
        }) { req ->
            seenAuth = req["Authorization"]
            NetFlowMockResponse.success("""{"id":1}""")
        }

        val res = client.call { path = "me" }.response()

        assertEquals(200, res.code)
        assertEquals("Bearer good", seenAuth)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }

    @Test
    fun `expired token triggers refresh then retries with the new token`() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { raw ->
                val r = raw.call { path = "auth/refresh"; skipAuth() }.response()
                if (r.code == 200) BearerTokens("fresh", "r2") else null
            }
        }) { req ->
            when {
                req.path == "auth/refresh" -> NetFlowMockResponse.success("""{"access":"fresh"}""")
                req["Authorization"] == "Bearer expired" -> NetFlowMockResponse.unauthorized()
                req["Authorization"] == "Bearer fresh" -> NetFlowMockResponse.success("""{"id":1}""")
                else -> NetFlowMockResponse.notFound()
            }
        }

        val res = client.call { path = "me" }.response()

        assertEquals(200, res.code)
        assertEquals(AuthState.Authenticated, client.authState.value)
        client.assertCalledTimes("me", HttpMethod.Get, times = 2)
        client.assertCalledTimes("auth/refresh", HttpMethod.Get, times = 1)
    }

    @Test
    fun `refresh returning null surfaces the original 401 and moves to Unauthenticated`() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { null }
        }) { NetFlowMockResponse.unauthorized() }

        val res = client.call { path = "me" }.response()

        assertEquals(401, res.code)
        assertEquals(AuthState.Unauthenticated, client.authState.value)
    }

    @Test
    fun `only one refresh happens for concurrent 401s`() = runTest {
        var refreshCalls = 0
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { refreshCalls++; BearerTokens("fresh", "r2") }
        }) { req ->
            if (req["Authorization"] == "Bearer fresh") NetFlowMockResponse.success("""{"ok":1}""")
            else NetFlowMockResponse.unauthorized()
        }

        val results = List(5) { async { client.call { path = "me" }.response() } }.awaitAll()

        assertEquals(1, refreshCalls)
        results.forEach { assertEquals(200, it.code) }
    }

    @Test
    fun `skipAuth request sends no Authorization header and does not refresh`() = runTest {
        var seenAuth: String? = "unset"
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { error("should not refresh") }
        }) { req ->
            seenAuth = req["Authorization"]
            NetFlowMockResponse.unauthorized()
        }

        val res = client.call { path = "public"; skipAuth() }.response()

        assertEquals(401, res.code)
        assertEquals(null, seenAuth)
    }

    @Test
    fun `client without auth block is byte-identical - no header - no refresh - 401 passes through`() = runTest {
        var seenAuth: String? = "unset"
        val client = MockNetFlowClient { req ->
            seenAuth = req["Authorization"]
            NetFlowMockResponse.unauthorized()
        }

        val res = client.call { path = "me" }.response()

        assertEquals(401, res.code)
        assertEquals(null, seenAuth)
        assertEquals(AuthState.Unknown, client.authState.value)
    }

    @Test
    fun `setTokens then request uses the manual token`() = runTest {
        var seenAuth: String? = null
        val client = MockNetFlowClient(auth = {
            loadTokens { null }
            refreshTokens { null }
        }) { req -> seenAuth = req["Authorization"]; NetFlowMockResponse.success() }

        client.setTokens(BearerTokens("manual", "r"))
        client.call { path = "me" }.response()

        assertEquals("Bearer manual", seenAuth)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }

    @Test
    fun `clearTokens moves to Unauthenticated and drops the header`() = runTest {
        var seenAuth: String? = "unset"
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { null }
        }) { req -> seenAuth = req["Authorization"]; NetFlowMockResponse.success() }

        client.clearTokens()
        client.call { path = "me" }.response()

        assertEquals(null, seenAuth)
        assertEquals(AuthState.Unauthenticated, client.authState.value)
    }

    @Test
    fun `custom scheme is honoured`() = runTest {
        var seenAuth: String? = null
        val client = MockNetFlowClient(auth = {
            scheme = "Token"
            loadTokens { BearerTokens("abc") }
            refreshTokens { null }
        }) { req -> seenAuth = req["Authorization"]; NetFlowMockResponse.success() }

        client.call { path = "me" }.response()
        assertEquals("Token abc", seenAuth)
    }

    @Test
    fun `post-refresh retry that still 401s returns the 401 without a second refresh`() = runTest {
        var refreshCalls = 0
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { refreshCalls++; BearerTokens("stillbad", "r2") }
        }) { NetFlowMockResponse.unauthorized() }

        val res = client.call { path = "me" }.response()

        assertEquals(401, res.code)
        assertEquals(1, refreshCalls)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }
}

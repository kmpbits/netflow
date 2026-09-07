package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryTokenStorageTest {

    @Test
    fun `starts empty by default`() = runTest {
        assertNull(InMemoryTokenStorage().load())
    }

    @Test
    fun `returns the initial tokens`() = runTest {
        val storage = InMemoryTokenStorage(BearerTokens("a", "r"))
        assertEquals(BearerTokens("a", "r"), storage.load())
    }

    @Test
    fun `save then load round-trips - clear empties`() = runTest {
        val storage = InMemoryTokenStorage()
        storage.save(BearerTokens("x", "y"))
        assertEquals(BearerTokens("x", "y"), storage.load())
        storage.clear()
        assertNull(storage.load())
    }

    @Test
    fun `wires into auth as a seed source`() = runTest {
        val storage = InMemoryTokenStorage(BearerTokens("seed", "r"))
        var seenAuth: String? = null
        val client = MockNetFlowClient(auth = {
            storage(storage)
            refreshTokens { null }
        }) { req -> seenAuth = req["Authorization"]; NetFlowMockResponse.success() }

        client.call { path = "me" }.response()

        assertEquals("Bearer seed", seenAuth)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }
}

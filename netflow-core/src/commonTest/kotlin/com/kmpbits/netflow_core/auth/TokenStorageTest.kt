package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.client.RawNetFlowClient
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeTokenStorage(
    initial: BearerTokens? = null,
    private val failOnSave: Boolean = false,
) : TokenStorage {
    var stored: BearerTokens? = initial
    var loadCalls = 0
    var saveCalls = 0
    var clearCalls = 0

    override suspend fun load(): BearerTokens? {
        loadCalls++
        return stored
    }

    override suspend fun save(tokens: BearerTokens) {
        saveCalls++
        if (failOnSave) error("disk full")
        stored = tokens
    }

    override suspend fun clear() {
        clearCalls++
        stored = null
    }
}

class TokenStorageTest {

    private fun rawProvider(): () -> RawNetFlowClient = {
        RawNetFlowClient(MockNetFlowClient { NetFlowMockResponse.success() })
    }

    private fun config(
        storage: TokenStorage? = null,
        load: (suspend () -> BearerTokens?)? = null,
        refresh: (suspend RefreshScope.(RawNetFlowClient) -> BearerTokens?)? = null,
    ) = AuthConfig().apply {
        if (storage != null) storage(storage)
        if (load != null) loadTokens(load)
        if (refresh != null) refreshTokens(refresh)
    }

    @Test
    fun `seeds from storage when no loadTokens block`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("stored", "r"))
        val holder = TokenHolder(config(storage = store), rawProvider())

        holder.ensureSeeded()

        assertEquals("stored", holder.currentAccess())
        assertEquals(AuthState.Authenticated, holder.authState.value)
        assertEquals(1, store.loadCalls)
    }

    @Test
    fun `loadTokens block takes precedence over storage for seeding`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("stored", "r"))
        val holder = TokenHolder(
            config(storage = store, load = { BearerTokens("explicit", "r") }),
            rawProvider(),
        )

        holder.ensureSeeded()

        assertEquals("explicit", holder.currentAccess())
        assertEquals(0, store.loadCalls)
    }

    @Test
    fun `successful refresh persists the new tokens`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("old", "r1"))
        val holder = TokenHolder(
            config(storage = store, refresh = { BearerTokens("new", "r2") }),
            rawProvider(),
        )
        holder.ensureSeeded()

        holder.refresh(accessUsed = "old")

        assertEquals(BearerTokens("new", "r2"), store.stored)
        assertEquals(1, store.saveCalls)
    }

    @Test
    fun `setTokens persists`() = runTest {
        val store = FakeTokenStorage()
        val holder = TokenHolder(config(storage = store), rawProvider())

        holder.set(BearerTokens("manual", "r"))

        assertEquals(BearerTokens("manual", "r"), store.stored)
    }

    @Test
    fun `clearTokens wipes storage`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("a", "r"))
        val holder = TokenHolder(config(storage = store), rawProvider())
        holder.ensureSeeded()

        holder.clear()

        assertNull(store.stored)
        assertEquals(1, store.clearCalls)
    }

    @Test
    fun `failed refresh wipes storage`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("old", "r1"))
        val holder = TokenHolder(
            config(storage = store, refresh = { null }),
            rawProvider(),
        )
        holder.ensureSeeded()

        holder.refresh(accessUsed = "old")

        assertNull(store.stored)
        assertTrue(store.clearCalls >= 1)
        assertEquals(AuthState.Unauthenticated, holder.authState.value)
    }

    @Test
    fun `storage save failure does not break refresh`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("old", "r1"), failOnSave = true)
        val holder = TokenHolder(
            config(storage = store, refresh = { BearerTokens("new", "r2") }),
            rawProvider(),
        )
        holder.ensureSeeded()

        val result = holder.refresh(accessUsed = "old")

        assertEquals(BearerTokens("new", "r2"), result)
        assertEquals("new", holder.currentAccess())
        assertEquals(AuthState.Authenticated, holder.authState.value)
    }

    @Test
    fun `end to end - refreshed token survives via storage into a new client`() = runTest {
        val store = FakeTokenStorage(initial = BearerTokens("expired", "r1"))

        val client1 = MockNetFlowClient(auth = {
            storage(store)
            refreshTokens { BearerTokens("fresh", "r2") }
        }) { req ->
            if (req["Authorization"] == "Bearer fresh") NetFlowMockResponse.success("""{"ok":1}""")
            else NetFlowMockResponse.unauthorized()
        }

        client1.call { path = "me" }.response()
        assertEquals(BearerTokens("fresh", "r2"), store.stored)

        // a fresh client backed by the same storage starts already authenticated
        val client2 = MockNetFlowClient(auth = {
            storage(store)
            refreshTokens { null }
        }) { NetFlowMockResponse.success("""{"ok":1}""") }

        val res = client2.call { path = "me" }.response()
        assertEquals(200, res.code)
        assertEquals(AuthState.Authenticated, client2.authState.value)
    }
}

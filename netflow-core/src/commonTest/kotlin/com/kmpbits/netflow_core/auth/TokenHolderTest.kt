package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.client.RawNetFlowClient
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TokenHolderTest {

    private fun rawProvider(): () -> RawNetFlowClient = {
        RawNetFlowClient(MockNetFlowClient { NetFlowMockResponse.success() })
    }

    private fun config(
        load: (suspend () -> BearerTokens?)? = null,
        refresh: (suspend RefreshScope.(RawNetFlowClient) -> BearerTokens?)? = null,
        scheme: String = "Bearer",
    ) = AuthConfig().apply {
        this.scheme = scheme
        if (load != null) loadTokens(load)
        if (refresh != null) refreshTokens(refresh)
    }

    @Test
    fun `ensureSeeded with no loadTokens leaves state Unknown`() = runTest {
        val holder = TokenHolder(config(), rawProvider())
        holder.ensureSeeded()
        assertEquals(AuthState.Unknown, holder.authState.value)
        assertNull(holder.currentAccess())
    }

    @Test
    fun `ensureSeeded populates tokens and moves to Authenticated`() = runTest {
        val holder = TokenHolder(config(load = { BearerTokens("a", "r") }), rawProvider())
        holder.ensureSeeded()
        assertEquals(AuthState.Authenticated, holder.authState.value)
        assertEquals("a", holder.currentAccess())
    }

    @Test
    fun `ensureSeeded runs loadTokens exactly once across calls`() = runTest {
        var calls = 0
        val holder = TokenHolder(config(load = { calls++; BearerTokens("a") }), rawProvider())
        holder.ensureSeeded()
        holder.ensureSeeded()
        assertEquals(1, calls)
    }

    @Test
    fun `refresh swaps tokens and stays Authenticated`() = runTest {
        val holder = TokenHolder(
            config(
                load = { BearerTokens("old", "r1") },
                refresh = { BearerTokens("new", "r2") },
            ),
            rawProvider(),
        )
        holder.ensureSeeded()
        val result = holder.refresh(accessUsed = "old")
        assertEquals(BearerTokens("new", "r2"), result)
        assertEquals("new", holder.currentAccess())
        assertEquals(AuthState.Authenticated, holder.authState.value)
    }

    @Test
    fun `refresh returning null moves to Unauthenticated and clears tokens`() = runTest {
        val holder = TokenHolder(
            config(load = { BearerTokens("old", "r1") }, refresh = { null }),
            rawProvider(),
        )
        holder.ensureSeeded()
        assertNull(holder.refresh("old"))
        assertEquals(AuthState.Unauthenticated, holder.authState.value)
        assertNull(holder.currentAccess())
    }

    @Test
    fun `refresh throwing is treated as null`() = runTest {
        val holder = TokenHolder(
            config(load = { BearerTokens("old", "r1") }, refresh = { error("boom") }),
            rawProvider(),
        )
        holder.ensureSeeded()
        assertNull(holder.refresh("old"))
        assertEquals(AuthState.Unauthenticated, holder.authState.value)
    }

    @Test
    fun `refresh with no refresh token fails fast without calling the block`() = runTest {
        var blockCalls = 0
        val holder = TokenHolder(
            config(
                load = { BearerTokens("old", refreshToken = null) },
                refresh = { blockCalls++; BearerTokens("x") },
            ),
            rawProvider(),
        )
        holder.ensureSeeded()
        assertNull(holder.refresh("old"))
        assertEquals(0, blockCalls)
        assertEquals(AuthState.Unauthenticated, holder.authState.value)
    }

    @Test
    fun `concurrent refreshes trigger the block once - single-flight`() = runTest {
        var blockCalls = 0
        val gate = CompletableDeferred<Unit>()
        val holder = TokenHolder(
            config(
                load = { BearerTokens("old", "r1") },
                refresh = { blockCalls++; gate.await(); BearerTokens("new", "r2") },
            ),
            rawProvider(),
        )
        holder.ensureSeeded()

        val jobs = List(5) { async { holder.refresh(accessUsed = "old") } }
        delay(50)
        gate.complete(Unit)
        val results = jobs.awaitAll()

        assertEquals(1, blockCalls)
        results.forEach { assertEquals(BearerTokens("new", "r2"), it) }
    }

    @Test
    fun `refresh is a no-op returning current tokens when access already rotated`() = runTest {
        var blockCalls = 0
        val holder = TokenHolder(
            config(
                load = { BearerTokens("old", "r1") },
                refresh = { blockCalls++; BearerTokens("new", "r2") },
            ),
            rawProvider(),
        )
        holder.ensureSeeded()
        holder.refresh(accessUsed = "old")
        val second = holder.refresh(accessUsed = "old")
        assertEquals(BearerTokens("new", "r2"), second)
        assertEquals(1, blockCalls)
    }

    @Test
    fun `set moves to Authenticated and stores tokens`() = runTest {
        val holder = TokenHolder(config(), rawProvider())
        holder.set(BearerTokens("manual", "r"))
        assertEquals("manual", holder.currentAccess())
        assertEquals(AuthState.Authenticated, holder.authState.value)
    }

    @Test
    fun `clear drops tokens and moves to Unauthenticated`() = runTest {
        val holder = TokenHolder(config(load = { BearerTokens("a") }), rawProvider())
        holder.ensureSeeded()
        holder.clear()
        assertNull(holder.currentAccess())
        assertEquals(AuthState.Unauthenticated, holder.authState.value)
    }

    @Test
    fun `scheme mirrors config`() {
        val holder = TokenHolder(config(scheme = "Token"), rawProvider())
        assertEquals("Token", holder.scheme)
    }
}

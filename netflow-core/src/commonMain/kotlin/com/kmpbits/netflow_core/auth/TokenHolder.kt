package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.client.RawNetFlowClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Owns the in-memory token copy for one [com.kmpbits.netflow_core.client.NetFlowClient],
 * serialises refreshes so concurrent 401s cause a single refresh call, drives
 * [authState], and (when configured) mirrors every change into a [TokenStorage].
 */
internal class TokenHolder(
    private val config: AuthConfig,
    private val rawClientProvider: () -> RawNetFlowClient,
) {
    val scheme: String get() = config.scheme

    private val storage: TokenStorage? = config.tokenStorage

    private val mutex = Mutex()
    private var tokens: BearerTokens? = null
    private var seeded = false

    private val _authState = MutableStateFlow(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun ensureSeeded() {
        if (seeded) return
        mutex.withLock {
            if (seeded) return
            tokens = config.loadTokensBlock?.invoke() ?: loadFromStorage()
            _authState.value = if (tokens != null) AuthState.Authenticated else AuthState.Unknown
            seeded = true
        }
    }

    fun currentAccess(): String? = tokens?.accessToken

    /**
     * @param accessUsed the access token the failing request actually sent.
     * @return the new tokens, or null when the session is now dead.
     */
    suspend fun refresh(accessUsed: String?): BearerTokens? = mutex.withLock {
        val current = tokens
        // another coroutine refreshed while we waited for the lock
        if (current != null && current.accessToken != accessUsed) return current

        val block = config.refreshTokensBlock
        val refreshTok = current?.refreshToken
        if (block == null || refreshTok == null || current == null) {
            failLocked()
            return null
        }

        val scope = RefreshScope(current.accessToken, refreshTok)
        val new = try {
            block(scope, rawClientProvider())
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            null
        }

        if (new == null) {
            failLocked()
            return null
        }

        tokens = new
        seeded = true
        _authState.value = AuthState.Authenticated
        persist(new)
        new
    }

    suspend fun set(newTokens: BearerTokens) = mutex.withLock {
        tokens = newTokens
        seeded = true
        _authState.value = AuthState.Authenticated
        persist(newTokens)
    }

    suspend fun clear() = mutex.withLock {
        tokens = null
        seeded = true
        _authState.value = AuthState.Unauthenticated
        wipe()
    }

    private suspend fun failLocked() {
        tokens = null
        _authState.value = AuthState.Unauthenticated
        wipe()
    }

    private suspend fun loadFromStorage(): BearerTokens? =
        try {
            storage?.load()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            null
        }

    private suspend fun persist(tokens: BearerTokens) {
        try {
            storage?.save(tokens)
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            // best-effort: the in-memory token is still valid
        }
    }

    private suspend fun wipe() {
        try {
            storage?.clear()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            // best-effort
        }
    }
}

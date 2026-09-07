package com.kmpbits.netflow_core.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [TokenStorage] that keeps tokens in memory only — they do not survive a
 * process restart. Useful for tests, and for apps that deliberately want a
 * fresh login every launch.
 *
 * For real persistence use `SettingsTokenStorage` from `netflow-token-storage`,
 * or implement [TokenStorage] over your own store.
 */
class InMemoryTokenStorage(initial: BearerTokens? = null) : TokenStorage {

    private val mutex = Mutex()
    private var tokens: BearerTokens? = initial

    override suspend fun load(): BearerTokens? = mutex.withLock { tokens }

    override suspend fun save(tokens: BearerTokens) = mutex.withLock { this.tokens = tokens }

    override suspend fun clear() = mutex.withLock { tokens = null }
}

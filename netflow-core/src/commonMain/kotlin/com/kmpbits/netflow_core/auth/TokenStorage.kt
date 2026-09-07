package com.kmpbits.netflow_core.auth

/**
 * Persists [BearerTokens] across process restarts. Register one with
 * `auth { storage(...) }` and NetFlow seeds from [load] on the first request and
 * writes back automatically on every token change (refresh, `setTokens`) and on
 * session end (`clearTokens`, a failed refresh).
 *
 * NetFlow ships only this interface — no encryption. Back it with an encrypted
 * store (iOS Keychain, Android Keystore-backed storage, or an encrypted
 * `multiplatform-settings` backend).
 *
 * Implementations should be safe to call from any coroutine; NetFlow only ever
 * calls them while holding its internal refresh lock.
 */
interface TokenStorage {

    /** Returns the stored tokens, or null when nothing is persisted. */
    suspend fun load(): BearerTokens?

    /** Persists [tokens], replacing anything already stored. */
    suspend fun save(tokens: BearerTokens)

    /** Removes any persisted tokens. */
    suspend fun clear()
}

package com.kmpbits.netflow_core.auth

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_core.client.RawNetFlowClient
import kotlin.time.Duration

/** Receiver for `refreshTokens { }` — exposes the tokens the failing request carried. */
class RefreshScope internal constructor(
    val accessToken: String,
    val refreshToken: String?,
)

/**
 * Configures automatic bearer-token auth on a [com.kmpbits.netflow_core.client.NetFlowClient].
 * NetFlow stays backend-agnostic: you provide the two lambdas, NetFlow provides
 * the attach / detect-401 / single-flight-refresh / retry-once orchestration.
 */
@NetFlowMarker
class AuthConfig internal constructor() {

    /** Header scheme; the header becomes `Authorization: $scheme $accessToken`. */
    var scheme: String = "Bearer"

    /**
     * When set, NetFlow refreshes *before* sending a request if the access token's
     * JWT `exp` claim is within this window of expiring, saving the failed 401
     * round-trip. Requires a `refreshTokens { }` block and a parseable JWT access
     * token; opaque tokens silently fall back to the reactive 401 path, which
     * always stays active as the safety net. `null` (default) disables it.
     */
    var refreshLeeway: Duration? = null

    internal var loadTokensBlock: (suspend () -> BearerTokens?)? = null
        private set

    internal var refreshTokensBlock: (suspend RefreshScope.(RawNetFlowClient) -> BearerTokens?)? = null
        private set

    internal var tokenStorage: TokenStorage? = null
        private set

    /** Called once, lazily, on the first request, to seed the in-memory token holder. */
    fun loadTokens(block: suspend () -> BearerTokens?) {
        loadTokensBlock = block
    }

    /**
     * Registers a [TokenStorage] for automatic persistence. NetFlow seeds from
     * [TokenStorage.load] (unless [loadTokens] is also set, which takes
     * precedence for seeding) and calls [TokenStorage.save] / [TokenStorage.clear]
     * on every token change and on session end.
     */
    fun storage(storage: TokenStorage) {
        tokenStorage = storage
    }

    /**
     * Called when a request returns 401. Return new [BearerTokens], or `null` to
     * end the session ([AuthState.Unauthenticated]). Persist inside this block if
     * you need durability — NetFlow only keeps an in-memory copy.
     */
    fun refreshTokens(block: suspend RefreshScope.(RawNetFlowClient) -> BearerTokens?) {
        refreshTokensBlock = block
    }
}

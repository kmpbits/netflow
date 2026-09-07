package com.kmpbits.netflow_core.auth

/**
 * The tokens NetFlow attaches to outgoing requests. [refreshToken] is optional —
 * some backends issue only a short-lived access token.
 */
data class BearerTokens(
    val accessToken: String,
    val refreshToken: String? = null,
)

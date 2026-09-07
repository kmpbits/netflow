package com.kmpbits.netflow_core.auth

/**
 * The session state NetFlow derives from token activity.
 *
 * - [Unknown]: no `loadTokens` seed has run yet.
 * - [Authenticated]: a token is held (seeded, set via `setTokens`, or refreshed).
 * - [Unauthenticated]: `refreshTokens { }` returned null, or `clearTokens()` was called.
 */
enum class AuthState { Unknown, Authenticated, Unauthenticated }

package com.kmpbits.sample.android.core.di

import com.kmpbits.netflow_core.auth.AuthConfig
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.auth.TokenStorage
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.extensions.netflowClient
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.dto.RefreshRequest
import com.kmpbits.sample.android.data.remote.createAuthApi
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

/**
 * A NetFlowClient wired for a token-authenticated backend. Every request gets a
 * bearer token; a 401 triggers a single-flight refresh + retry; and with
 * [AuthConfig.refreshLeeway] set, a soon-to-expire JWT is refreshed before the
 * request even goes out.
 *
 * The [storage] here is the platform secure store (see [createTokenStorage] —
 * iOS Keychain / Android EncryptedSharedPreferences). For tests or a
 * login-every-launch app, pass `InMemoryTokenStorage()` instead.
 */
fun sampleAuthConfig(storage: TokenStorage): AuthConfig.() -> Unit = {
    refreshLeeway = 30.seconds

    // Seed from storage on the first request. No `loadTokens { }` needed — storage(...) does it.
    storage(storage)

    refreshTokens { raw ->
        // `raw` is auth-free, so this refresh call can't recurse into another refresh.
        // refreshToken is non-null here: NetFlow only calls this block when one is held.
        when (val res = raw.createAuthApi().refresh(RefreshRequest(refreshToken!!))) {
            is AsyncState.Success -> BearerTokens(res.data.accessToken, res.data.refreshToken)
            else -> null   // refresh token is dead -> AuthState.Unauthenticated
        }
    }
}

val authNetworkModule = module {

    // Platform secure store. For tests / login-every-launch: InMemoryTokenStorage().
    single<TokenStorage> { createTokenStorage() }

    single(named("auth")) {
        netflowClient {
            baseUrl = "https://api.example.com"
            logLevel = LogLevel.Body
            auth(sampleAuthConfig(get()))
        }
    }
}

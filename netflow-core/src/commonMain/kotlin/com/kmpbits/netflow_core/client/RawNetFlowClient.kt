package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The [NetFlowClient] handed to `auth { refreshTokens { } }`. It forwards plain
 * requests to [delegate] but carries no auth interceptor and no token holder, so
 * a refresh call cannot recurse into another refresh.
 *
 * [delegate] must itself be an auth-free client (see `NetFlowClientImpl` /
 * `MockNetFlowClient`, which build it wrapping a copy configured with no `auth { }`).
 */
class RawNetFlowClient internal constructor(
    private val delegate: NetFlowClient,
) : NetFlowClient {

    override fun call(builder: RequestBuilder.() -> Unit): NetFlowRequest = delegate.call(builder)

    private val _authState = MutableStateFlow(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun setTokens(tokens: BearerTokens) = Unit
    override suspend fun clearTokens() = Unit
}

package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest
import kotlinx.coroutines.flow.StateFlow

interface NetFlowClient {

    /**
     * This method should be called for every request.
     *
     * @param builder Have the customization of the request like the path and headers.
     * @return A [NetFlowRequest] that can be used to deserialize the response.
     */
    fun call(builder: RequestBuilder. () -> Unit = {}): NetFlowRequest

    /**
     * The session state NetFlow derives from token activity. Permanently
     * [AuthState.Unknown] unless an `auth { }` block is configured on the client.
     */
    val authState: StateFlow<AuthState>

    /** Store [tokens] and move to [AuthState.Authenticated]. No-op without `auth { }`. */
    suspend fun setTokens(tokens: BearerTokens)

    /** Drop the held tokens and move to [AuthState.Unauthenticated]. No-op without `auth { }`. */
    suspend fun clearTokens()
}

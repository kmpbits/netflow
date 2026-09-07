package com.kmpbits.sample.android.data.remote

import com.kmpbits.netflow_annotations.Body
import com.kmpbits.netflow_annotations.NetFlowApi
import com.kmpbits.netflow_annotations.POST
import com.kmpbits.netflow_annotations.SkipAuth
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.dto.LoginRequest
import com.kmpbits.sample.android.data.dto.RefreshRequest
import com.kmpbits.sample.android.data.dto.TokenResponse

/**
 * The unauthenticated endpoints. Both carry `@SkipAuth` so the generated code
 * never attaches a bearer token and never tries to refresh on a 401 — a refresh
 * that itself needed auth would be a chicken-and-egg loop.
 */
@NetFlowApi
interface AuthApi {

    @SkipAuth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AsyncState<TokenResponse>

    @SkipAuth
    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AsyncState<TokenResponse>
}

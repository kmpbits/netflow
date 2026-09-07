package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.auth.TokenHolder
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.logging.Logging
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * A single prepared request.
 *
 * When a [tokenHolder] is present (the client has an `auth { }` block) and the
 * request is not opted out via [RequestBuilder.skipAuth], [response] attaches the
 * bearer token, and on a `401` runs a single-flight refresh and retries once.
 *
 * Note: on Android the client's shared header list is what
 * `CustomHeaderInterceptor` re-applies at request time, and that same list backs
 * [RequestBuilder.headers], so mutating it here keeps the interceptor consistent.
 * Auth tokens are client-global, so this shared mutation is safe.
 */
class NetFlowRequest internal constructor(
    @PublishedApi
    internal val builder: RequestBuilder,
    @PublishedApi
    internal val client: HttpEngineAdapter,
    @PublishedApi
    internal val requestBuilder: InternalHttpRequestBuilder,
    @PublishedApi
    internal val logLevel: LogLevel,
    internal val tokenHolder: TokenHolder? = null,
) {
    val immutableRequestBuilder = ImmutableRequestBuilder(
        preCall = builder.preCall,
        headers = builder.headers,
        builder = builder
    )

    private var refreshedOnce = false

    val method: HttpMethod
        get() = builder.method

    val headers: List<Pair<HttpHeader, String>>
        get() = builder.headers

    fun updateUrl() {
        requestBuilder.updateUrl(builder)
    }

    suspend fun response(): NetFlowResponse {
        val holder = tokenHolder
        if (holder == null || builder.skipAuth) return dispatchWithRetry()

        holder.ensureSeeded()
        holder.ensureFresh()

        var accessUsed = holder.currentAccess()
        applyAuthHeader(holder.scheme, accessUsed)

        var res = dispatchWithRetry()

        if (res.code == 401 && !refreshedOnce) {
            refreshedOnce = true
            val new = holder.refresh(accessUsed) ?: return res
            accessUsed = new.accessToken
            applyAuthHeader(holder.scheme, accessUsed)
            res = dispatchWithRetry()
        }
        return res
    }

    private fun applyAuthHeader(scheme: String, token: String?) {
        builder.headers.removeAll {
            it.first.header.equals(HttpHeader.AUTHORIZATION.header, ignoreCase = true)
        }
        if (token != null) {
            builder.headers.add(Header(HttpHeader.AUTHORIZATION, "$scheme $token"))
        }
        requestBuilder.updateHeaders(builder)
    }

    private suspend fun dispatchWithRetry(): NetFlowResponse {
        val retryBuilder = builder.retryBuilder
        val times = retryBuilder.times

        var lastError: Exception? = null

        for (attempt in 0 until times.times) {
            logRequest(attempt)

            try {
                val response = client.call(requestBuilder, builder)
                logResponse(response)
                return response
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastError = e

                val shouldRetry = retryBuilder.retryOn?.invoke(e) ?: true

                if (!shouldRetry) {
                    val response = NetFlowResponse(
                        code = 500,
                        headers = builder.headers,
                        body = null,
                        errorBody = e.message
                    )
                    logResponse(response)
                    return response
                }

                if (attempt < times.times - 1) {
                    delay(retryBuilder.delay)
                }
            }
        }

        // If we get here, all attempts failed
        val response = NetFlowResponse(
            code = 500,
            headers = builder.headers,
            body = null,
            errorBody = lastError?.message ?: "Unknown error"
        )

        logResponse(response)
        return response
    }


    private fun logRequest(attempt: Int) {
        Logging.logRequest(requestBuilder, attempt, logLevel)
    }

    private fun logResponse(response: NetFlowResponse) {
        Logging.logResponse(requestBuilder, response, logLevel)
    }
}

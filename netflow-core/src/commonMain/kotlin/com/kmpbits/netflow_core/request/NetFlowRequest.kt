package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.auth.TokenHolder
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.logging.Logging
import com.kmpbits.netflow_core.platform.HttpEngineAdapter
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * A single prepared request.
 *
 * When a [tokenHolder] is present (the client has an `auth { }` block) and the
 * request is not opted out via [RequestBuilder.skipAuth], [response] attaches the
 * bearer token, and on a `401` runs a single-flight refresh and retries once.
 *
 * Note: the client's shared header list backs [RequestBuilder.headers] and is
 * applied to the platform request by `RequestBuilder.build()` on both engines, so
 * mutating it here (e.g. to attach the auth header) reaches the actual request.
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

    /**
     * Streams the response body as it arrives. Auth and retry apply only **before the first byte**:
     * once a chunk has been emitted a failure is propagated, never replayed. Non-2xx throws
     * [HttpException]; a 401 triggers one single-flight token refresh and retry, like [response].
     * Only the request is logged (there is no buffered response to log).
     */
    fun stream(): Flow<ByteArray> = flow {
        val holder = tokenHolder
        if (holder == null || builder.skipAuth) {
            emitAll(streamWithRetry())
            return@flow
        }

        holder.ensureSeeded()
        holder.ensureFresh()

        val accessUsed = holder.currentAccess()
        applyAuthHeader(holder.scheme, accessUsed)

        var started = false
        try {
            streamWithRetry().collect { started = true; emit(it) }
        } catch (e: HttpException) {
            if (e.code != 401 || started || refreshedOnce) throw e
            refreshedOnce = true
            val new = holder.refresh(accessUsed) ?: throw e
            applyAuthHeader(holder.scheme, new.accessToken)
            emitAll(streamWithRetry())
        }
    }

    private fun streamWithRetry(): Flow<ByteArray> = flow {
        val retryBuilder = builder.retryBuilder
        val attempts = retryBuilder.times.times
        var attempt = 0

        while (true) {
            logRequest(attempt)
            var emitted = false
            try {
                client.stream(requestBuilder, builder).collect { emitted = true; emit(it) }
                return@flow
            } catch (e: Exception) {
                // HttpException is a server answer, not a transport failure: never retried (same as call()).
                if (e is CancellationException || emitted || e is HttpException) throw e

                val shouldRetry = retryBuilder.retryOn?.invoke(e) ?: true
                attempt++
                if (!shouldRetry || attempt >= attempts) throw e
                delay(retryBuilder.delay)
            }
        }
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

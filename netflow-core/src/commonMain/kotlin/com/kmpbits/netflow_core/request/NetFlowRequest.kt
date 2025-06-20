package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.logging.Logging
import com.kmpbits.netflow_core.platform.InternalHttpClient
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

class NetFlowRequest internal constructor(
    @PublishedApi
    internal val builder: RequestBuilder,
    @PublishedApi
    internal val client: InternalHttpClient,
    @PublishedApi
    internal val requestBuilder: InternalHttpRequestBuilder,
    @PublishedApi
    internal val logLevel: LogLevel
) {
    val immutableRequestBuilder = ImmutableRequestBuilder(
        preCall = builder.preCall,
        headers = builder.headers,
        builder = builder
    )

    val method: HttpMethod
        get() = builder.method

    val headers: List<Pair<HttpHeader, String>>
        get() = builder.headers

    suspend fun response(): NetFlowResponse {
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

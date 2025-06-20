package com.kmpbits.netflow_core.logging

import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse

internal object Logging {

    fun logRequest(
        request: InternalHttpRequestBuilder,
        attempt: Int,
        logLevel: LogLevel
    ) {
        val url = request.url
        val method = request.method
        val path = request.path
        val query = request.query
        val host = request.host
        val headers = request.headers

        val requestLog = buildString {
            append("\n---------- REQUEST ---------->\n")
            append("$url\n\n")
            append("$method $path?$query HTTP/1.1\n")
            append("Host: $host\n")

            if (logLevel != LogLevel.Basic)
                append("Headers: ${headers.joinToString(", ") { "${it.first}: ${it.second}" }}")

            append("Attempting...: $attempt\n")
        }

        if (logLevel != LogLevel.None)
            println(requestLog)
    }

    fun logResponse(
        request: InternalHttpRequestBuilder,
        response: NetFlowResponse,
        logLevel: LogLevel
    ) {
        val url = request.url
        val path = request.path
        val query = request.query
        val host = request.host
        val headers = request.headers
        val statusCode = response.code

        val responseLog = buildString {
            append("\n<---------- RESPONSE ----------\n")
            append("$url\n\n")
            append("HTTP $statusCode $path?$query\n")
            append("Host: $host\n")

            if (logLevel != LogLevel.Basic)
                append("Headers: ${headers.joinToString(", ") { "${it.first}: ${it.second}" }}")

            append("Success: ${ if (response.isSuccess) "Yes" else "No" }\n")

            if (logLevel == LogLevel.Body) {
                response.body?.let {
                    try {
                        append("\n$it\n")
                    } catch (_: Exception) {
                        append("\nCan't render body; not UTF-8 encoded\n")
                    }
                }
                response.errorBody?.let {
                    append("\nError: ${it}\n")
                }
            }
            append("<------------------------\n")
        }

        if (logLevel != LogLevel.None)
            println(responseLog)
    }
}
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
        val query = request.query.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: ""
        val host = request.host
        val headers = request.headers

        val requestLog = buildString {
            append("\n[NETFLOW] ---------- REQUEST ---------->\n")
            append("[NETFLOW] -> $url\n\n")
            append("[NETFLOW] -> $method $path$query HTTP/1.1\n")
            append("[NETFLOW] -> Host: $host\n\n")

            if (logLevel != LogLevel.Basic) {
                append("[NETFLOW] ----Headers----\n")
                append(headers.joinToString("\n") { " [NETFLOW]  - ${it.first.header}: ${it.second}" })
                append("\n\n")
            }

            append("[NETFLOW] -> Attempting...: ${attempt + 1}\n")
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
        val query = request.query.takeIf { it.isNotEmpty() }?.let { "?$it" } ?: ""
        val host = request.host
        val headers = request.headers
        val statusCode = response.code

        val responseLog = buildString {
            append("\n[NETFLOW] <---------- RESPONSE ----------\n")
            append("[NETFLOW] -> $url\n\n")
            append("[NETFLOW] -> HTTP $statusCode $path$query\n")
            append("[NETFLOW] -> Host: $host\n\n")

            if (logLevel != LogLevel.Basic) {
                append("[NETFLOW] ----Headers----\n")
                append(headers.joinToString("\n") { " - ${it.first.header}: ${it.second}" })
                append("\n\n")
            }

            append("[NETFLOW] -> Success: ${if (response.isSuccess) "Yes" else "No"}\n")

            if (logLevel == LogLevel.Body) {
                response.body?.let {
                    try {
                        append("\n[NETFLOW] $it\n")
                    } catch (_: Exception) {
                        append("\n[NETFLOW] Can't render body; not UTF-8 encoded\n")
                    }
                }
                response.errorBody?.let {
                    append("\n[NETFLOW] Error: $it\n")
                }
            }

            append("[NETFLOW] <------------------------\n")
        }

        if (logLevel != LogLevel.None)
            println(responseLog)
    }
}

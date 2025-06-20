package com.kmpbits.netflow_core.logging

import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder

internal object Logging {

    fun logRequest(request: InternalHttpRequestBuilder) {
        val url = request.url
        val method = request.method
        val path = request.path
        val query = request.query
        val host = request.host

        val requestLog = buildString {
            append("\n---------- REQUEST ---------->\n")
            append("$url\n\n")
            append("$method $path?$query HTTP/1.1\n")
            append("Host: $host\n")
        }

        println(requestLog)
    }
}
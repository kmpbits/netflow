package com.kmpbits.netflow_core.platform

import okhttp3.Request

internal actual class InternalHttpRequestBuilder(
    val request: Request
) {
    actual val url: String
        get() = request.url.toString()

    actual val method: String
        get() = request.method

    actual val path: String
        get() = request.url.encodedPath

    actual val query: String
        get() = request.url.query.orEmpty()

    actual val host: String
        get() = request.url.host
}
package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.extensions.urlWithPath
import okhttp3.Request

internal actual class InternalHttpRequestBuilder(
    val requestBuilder: Request.Builder
) {
    private val request: Request
        get() = requestBuilder.build()

    internal actual val url: String
        get() = request.url.toString()

    internal actual val method: String
        get() = request.method

    internal actual val path: String
        get() = request.url.encodedPath

    internal actual val query: String
        get() = request.url.query.orEmpty()

    internal actual val host: String
        get() = request.url.host

    internal actual val headers: Headers
        get() = request.headers.map {
            Header(HttpHeader.custom(it.first), it.second)
        }.toMutableList()

    internal actual fun updateUrl(builder: RequestBuilder) {
        val url = urlWithPath(
            url,
            builder.path,
            builder.method,
            builder.parameters
        )

        requestBuilder.url(url)
    }
}
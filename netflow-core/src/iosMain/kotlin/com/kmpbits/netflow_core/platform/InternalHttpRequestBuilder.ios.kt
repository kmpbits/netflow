package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.enums.HttpHeader
import platform.Foundation.HTTPMethod
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.allHTTPHeaderFields

internal actual class InternalHttpRequestBuilder(
    internal val request: NSMutableURLRequest
) {
    internal actual val url: String
        get() = request.URL?.absoluteString.orEmpty()

    internal actual val method: String
        get() = request.HTTPMethod

    internal actual val path: String
        get() = request.URL?.path.orEmpty()

    internal actual val query: String
        get() = request.URL?.query.orEmpty()

    internal actual val host: String
        get() = request.URL?.host.orEmpty()

    internal actual val headers: Headers
        get() = request.allHTTPHeaderFields?.map {
            Header(HttpHeader.custom(it.key.toString()), it.value.toString())
        }?.toMutableList() ?: mutableListOf()
}
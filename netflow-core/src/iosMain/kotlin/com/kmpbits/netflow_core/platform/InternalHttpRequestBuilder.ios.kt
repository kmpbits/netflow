package com.kmpbits.netflow_core.platform

import platform.Foundation.HTTPMethod
import platform.Foundation.NSMutableURLRequest

internal actual class InternalHttpRequestBuilder(
    internal val request: NSMutableURLRequest
) {
    actual val url: String
        get() = request.URL?.absoluteString.orEmpty()

    actual val method: String
        get() = request.HTTPMethod

    actual val path: String
        get() = request.URL?.path.orEmpty()

    actual val query: String
        get() = request.URL?.query.orEmpty()

    actual val host: String
        get() = request.URL?.host.orEmpty()
}
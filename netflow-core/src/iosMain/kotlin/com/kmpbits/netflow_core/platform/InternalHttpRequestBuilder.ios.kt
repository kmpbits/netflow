package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.extensions.urlWithPath
import com.kmpbits.netflow_core.interceptor.InterceptedRequest
import platform.Foundation.HTTPMethod
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.allHTTPHeaderFields
import platform.Foundation.setValue

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

    internal actual fun updateUrl(builder: RequestBuilder) {
        val newUrl = urlWithPath(
            builder.baseUrl,
            builder.path,
            builder.method,
            builder.parameters
        )
        request.setURL(NSURL.URLWithString(newUrl))
    }

    internal actual fun updateHeaders(builder: RequestBuilder) {
        builder.headers.forEach {
            // setValue:forHTTPHeaderField: replaces any existing value for this name
            request.setValue(it.second, forHTTPHeaderField = it.first.header)
        }
    }

    internal actual fun apply(request: InterceptedRequest) {
        this.request.setURL(NSURL.URLWithString(request.url))
        this.request.allHTTPHeaderFields = request.headers.associate { it.first.header to it.second }
    }
}
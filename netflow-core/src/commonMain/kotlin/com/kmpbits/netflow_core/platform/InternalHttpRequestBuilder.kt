package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Headers
import com.kmpbits.netflow_core.builders.RequestBuilder

internal expect class InternalHttpRequestBuilder {

    internal val url: String
    internal val method: String
    internal val path: String
    internal val query: String
    internal val host: String
    internal val headers: Headers

    internal fun updateUrl(builder: RequestBuilder)

    /**
     * Re-applies [builder]'s headers onto the platform request, replacing any
     * existing value for the same header name. Used to swap in a refreshed token
     * before a retry.
     */
    internal fun updateHeaders(builder: RequestBuilder)
}
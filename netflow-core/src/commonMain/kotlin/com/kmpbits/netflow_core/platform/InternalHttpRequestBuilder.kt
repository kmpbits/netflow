package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Headers

internal expect class InternalHttpRequestBuilder {

    internal val url: String
    internal val method: String
    internal val path: String
    internal val query: String
    internal val host: String
    internal val headers: Headers
}
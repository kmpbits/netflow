package com.kmpbits.netflow_core.platform

internal expect class InternalHttpRequestBuilder {

    val url: String
    val method: String
    val path: String
    val query: String
    val host: String
}
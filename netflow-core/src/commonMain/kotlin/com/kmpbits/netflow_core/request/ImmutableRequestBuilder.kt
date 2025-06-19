package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.builders.RequestBuilder

class ImmutableRequestBuilder internal constructor(
    @PublishedApi
    internal val builder: RequestBuilder,
    val preCall: (() -> Unit)?,
    val headers: List<Header>,
) {

    fun updateParameter(key: String, value: Any) = builder.updateParameter(key, value)
}
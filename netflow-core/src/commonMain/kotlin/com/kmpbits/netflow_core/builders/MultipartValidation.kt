package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.exceptions.NetFlowException

/**
 * Guards for a multipart request, run at the top of each platform's `build()`.
 * No-ops when the request is not multipart. "No parts" and blank names are
 * already rejected eagerly in [MultipartBuilder] / [multipart].
 */
internal fun RequestBuilder.validateMultipart() {
    if (parts.isEmpty()) return

    if (rawBody != null || body != null) {
        throw NetFlowException("Cannot combine multipart with a JSON body")
    }
    if (method !in setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
        throw NetFlowException("Multipart requires POST, PUT or PATCH")
    }
}

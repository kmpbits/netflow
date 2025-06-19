package com.kmpbits.netflow_core.extensions

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.exceptions.NetFlowException

internal fun String.toHttpMethod(): HttpMethod = when(this) {
    "GET" -> HttpMethod.Get
    "POST" -> HttpMethod.Post
    "PUT" -> HttpMethod.Put
    "DELETE" -> HttpMethod.Delete
    "PATCH" -> HttpMethod.Patch
    "HEAD" -> HttpMethod.Head
    else -> throw NetFlowException("Invalid HttpMethod: $this")
}

internal fun HttpMethod.toName() = when(this) {
    HttpMethod.Delete -> "DELETE"
    HttpMethod.Get -> "GET"
    HttpMethod.Head -> "HEAD"
    HttpMethod.Patch -> "PATCH"
    HttpMethod.Post -> "POST"
    HttpMethod.Put -> "PUT"
}
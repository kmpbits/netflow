package com.kmpbits.netflow_core.platform

import okhttp3.Request

internal actual class InternalHttpRequestBuilder(
    val requestBuilder: Request.Builder
)
package com.kmpbits.netflow_core.platform

import platform.Foundation.NSMutableURLRequest

internal actual class InternalHttpRequestBuilder(
    internal val request: NSMutableURLRequest
)
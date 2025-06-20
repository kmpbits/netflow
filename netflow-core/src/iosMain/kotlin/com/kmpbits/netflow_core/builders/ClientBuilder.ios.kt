package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.platform.InternalHttpClient
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import kotlin.time.DurationUnit

internal actual fun ClientBuilder.createClient(): InternalHttpClient {
    val config = NSURLSessionConfiguration.defaultSessionConfiguration.apply {
        timeoutIntervalForRequest = timeoutBuilder.connectionTimeout.toDouble(DurationUnit.SECONDS)
        timeoutIntervalForResource = timeoutBuilder.connectionTimeout.toDouble(DurationUnit.SECONDS)
    }
    val session = NSURLSession.sessionWithConfiguration(config)
    return InternalHttpClient(session)
}
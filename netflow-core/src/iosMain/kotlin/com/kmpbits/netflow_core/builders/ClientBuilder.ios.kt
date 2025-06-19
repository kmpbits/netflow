package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.platform.InternalHttpClient

internal actual fun createClient(): InternalHttpClient {
    // iOS doesn't need to do anything because URLSession is a singleton
    return InternalHttpClient()
}
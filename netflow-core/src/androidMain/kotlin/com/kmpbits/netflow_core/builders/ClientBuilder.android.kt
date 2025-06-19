package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.platform.InternalHttpClient
import okhttp3.OkHttpClient

internal actual fun createClient(): InternalHttpClient {
    val builder = OkHttpClient.Builder()
        .followRedirects(false)
        .retryOnConnectionFailure(true)

    val client = builder.build()
    return InternalHttpClient(client)
}
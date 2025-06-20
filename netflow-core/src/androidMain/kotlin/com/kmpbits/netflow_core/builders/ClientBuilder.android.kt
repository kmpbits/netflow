package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.client.CustomHeaderInterceptor
import com.kmpbits.netflow_core.platform.InternalHttpClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal actual fun ClientBuilder.createClient(): InternalHttpClient {
    val builder = OkHttpClient.Builder().apply {
        followRedirects(false)
        retryOnConnectionFailure(true)
        connectTimeout(timeoutBuilder.connectionTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        readTimeout(timeoutBuilder.readTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        writeTimeout(timeoutBuilder.writeTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        addInterceptor(CustomHeaderInterceptor(headers))
    }

    val client = builder.build()
    return InternalHttpClient(client)
}
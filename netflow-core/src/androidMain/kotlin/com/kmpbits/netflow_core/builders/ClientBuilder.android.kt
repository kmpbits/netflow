package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.platform.InternalHttpClient
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal actual fun ClientBuilder.createClient(): InternalHttpClient =
    InternalHttpClient(okHttpClientForTest())

/**
 * Builds the configured `OkHttpClient`. Separate from [createClient] so tests
 * can inspect the client without going through `InternalHttpClient`.
 */
internal fun ClientBuilder.okHttpClientForTest(): OkHttpClient {
    val builder = OkHttpClient.Builder().apply {
        followRedirects(this@okHttpClientForTest.followRedirects)
        retryOnConnectionFailure(true)
        connectTimeout(timeoutBuilder.connectionTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        readTimeout(timeoutBuilder.readTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
        writeTimeout(timeoutBuilder.writeTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    pinningConfig?.let { config ->
        val pinner = CertificatePinner.Builder()
        config.pins.forEach { pin ->
            pinner.add(pin.pattern, *pin.hashes.toTypedArray())
        }
        builder.certificatePinner(pinner.build())
    }

    return builder.build()
}

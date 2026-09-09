package com.kmpbits.sample.android.core.di

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.enums.RetryTimes
import com.kmpbits.netflow_core.extensions.netflowClient
import com.kmpbits.netflow_core.interceptor.NetFlowInterceptor
import org.koin.dsl.module
import kotlin.time.TimeSource

/**
 * Prints a trace id and the round-trip time for every call. Written once here,
 * it runs on both engines — no OkHttp `Interceptor`, no `NSURLSession` delegate.
 */
private val traceInterceptor = NetFlowInterceptor { chain ->
    val traceId = (100000..999999).random()
    val request = chain.request.newBuilder()
        .header(Header(HttpHeader.custom("X-Client-Trace-Id"), traceId.toString()))
        .build()

    val start = TimeSource.Monotonic.markNow()
    val response = chain.proceed(request)
    println("[NetFlow] #$traceId ${request.method} ${request.url} -> ${response.code} in ${start.elapsedNow()}")
    response
}

val networkModule = module {
    single {
        netflowClient {
            baseUrl = "https://jsonplaceholder.typicode.com"
            logLevel = LogLevel.Body

            defaultRetry {
                times = RetryTimes.THREE
            }

            addInterceptor(traceInterceptor)

            // Real SPKI pins for jsonplaceholder.typicode.com, fetched with the
            // openssl recipe from the README. Leaf + the Google Trust Services
            // intermediate (WE1) as backup, so the pin survives the leaf's own
            // rotation. Like any pinned host, these need refreshing if the CA
            // itself changes — that's the trade-off pinning makes explicit.
            pinning {
                pin(
                    "jsonplaceholder.typicode.com",
                    "sha256/fj/LGYZh+mUuNimcCT6b6V6MLFW1SIzcsM4hgwSwVB4=", // leaf
                    "sha256/kIdp6NNEd8wsugYyyIYFsi1ylMCED3hZbSR8ZFsa/A4=", // intermediate (WE1)
                )
            }
        }
    }
}
package com.kmpbits.sample.android.core.di

import com.kmpbits.netflow_core.enums.LogLevel
import com.kmpbits.netflow_core.enums.RetryTimes
import com.kmpbits.netflow_core.extensions.netflowClient
import org.koin.dsl.module

val networkModule = module {
    single { netflowClient {
        baseUrl = "https://jsonplaceholder.typicode.com"
        logLevel = LogLevel.Body

        defaultRetry {
            times = RetryTimes.THREE
        }
    } }
}
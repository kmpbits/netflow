package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * Observes and alters requests and responses, written once, running on both engines.
 *
 * Runs inside the retry loop and after auth: it sees the final `Authorization`
 * header and is called once per attempt.
 *
 * ```kotlin
 * val trace = NetFlowInterceptor { chain ->
 *     val request = chain.request.newBuilder()
 *         .header(Header(HttpHeader.custom("X-Trace-Id"), newTraceId()))
 *         .build()
 *     chain.proceed(request)
 * }
 * ```
 */
fun interface NetFlowInterceptor {

    suspend fun intercept(chain: Chain): NetFlowResponse

    interface Chain {
        /** The request as it arrived at this interceptor. */
        val request: InterceptedRequest

        /**
         * Hands [request] off to the rest of the chain and returns the response.
         * Not calling this short-circuits the chain: the network is never touched.
         */
        suspend fun proceed(request: InterceptedRequest): NetFlowResponse
    }
}

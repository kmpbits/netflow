package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * One link in the chain. The link at [index] hands off to the interceptor at
 * that position; once [index] runs past the end of the list, it calls
 * [terminal] — the real engine.
 */
internal class InterceptorChain(
    private val interceptors: List<NetFlowInterceptor>,
    private val index: Int,
    override val request: InterceptedRequest,
    private val terminal: suspend (InterceptedRequest) -> NetFlowResponse,
) : NetFlowInterceptor.Chain {

    override suspend fun proceed(request: InterceptedRequest): NetFlowResponse {
        if (index >= interceptors.size) return terminal(request)

        val next = InterceptorChain(interceptors, index + 1, request, terminal)
        return interceptors[index].intercept(next)
    }
}

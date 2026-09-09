package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * Um elo da cadeia. O elo em [index] entrega ao interceptor nessa posição; quando
 * [index] passa o fim da lista, chama [terminal] — o motor real.
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

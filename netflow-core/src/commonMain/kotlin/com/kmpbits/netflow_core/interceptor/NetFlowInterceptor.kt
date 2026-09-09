package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * Observa e altera pedidos e respostas, uma vez, nos dois motores.
 *
 * Corre dentro do ciclo de retry e depois do auth: vê o header `Authorization`
 * final e é chamado uma vez por tentativa.
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
        /** O pedido tal como chegou a este interceptor. */
        val request: InterceptedRequest

        /**
         * Entrega [request] ao resto da cadeia e devolve a resposta. Não chamar
         * faz curto-circuito: a rede não é tocada.
         */
        suspend fun proceed(request: InterceptedRequest): NetFlowResponse
    }
}

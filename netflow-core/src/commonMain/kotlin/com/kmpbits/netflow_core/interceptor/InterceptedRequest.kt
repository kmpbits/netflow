package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod

/**
 * O pedido tal como um [NetFlowInterceptor] o vê. Imutável — para o alterar,
 * usa [newBuilder] e devolve o resultado a `chain.proceed(...)`.
 *
 * O corpo é só de leitura: alterá-lo exigiria re-encoding no motor. Se vier a
 * ser preciso, é uma adição compatível.
 */
class InterceptedRequest internal constructor(
    val method: HttpMethod,
    val url: String,
    val headers: List<Header>,
    val body: String?,
) {

    fun newBuilder(): Builder = Builder(method, url, headers.toMutableList(), body)

    class Builder internal constructor(
        private val method: HttpMethod,
        private var url: String,
        private val headers: MutableList<Header>,
        private val body: String?,
    ) {

        fun url(url: String): Builder = apply { this.url = url }

        /** Adiciona [header], substituindo qualquer valor existente com o mesmo nome. */
        fun header(header: Header): Builder = apply {
            removeHeader(header.first)
            headers.add(header)
        }

        fun removeHeader(name: HttpHeader): Builder = apply {
            headers.removeAll { it.first.header.equals(name.header, ignoreCase = true) }
        }

        fun build(): InterceptedRequest =
            InterceptedRequest(method, url, headers.toList(), body)
    }
}

package com.kmpbits.netflow_core.interceptor

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.enums.HttpMethod

/**
 * The request as seen by a [NetFlowInterceptor]. Immutable — to change it,
 * use [newBuilder] and pass the result to `chain.proceed(...)`.
 *
 * The body is read-only: changing it would require re-encoding on the engine.
 * If that turns out to be needed, it's a backward-compatible addition.
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

        /** Adds [header], replacing any existing value with the same name. */
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

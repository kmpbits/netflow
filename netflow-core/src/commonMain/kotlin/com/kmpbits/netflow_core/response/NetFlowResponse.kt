package com.kmpbits.netflow_core.response

import com.kmpbits.netflow_core.alias.Header

/**
 * Uma resposta HTTP. Construível a partir de código de consumidor para que um
 * `NetFlowInterceptor` possa fazer curto-circuito da cadeia sem tocar na rede.
 *
 * [headers] são os headers **da resposta**.
 */
class NetFlowResponse(
    val code: Int,
    val headers: List<Header>,
    val body: String?,
    val errorBody: String?
) {
    val isSuccess: Boolean
        get() = code in (200..299)
}

package com.kmpbits.netflow_core.response

import com.kmpbits.netflow_core.alias.Header

/**
 * An HTTP response. Constructible from consumer code so a `NetFlowInterceptor`
 * can short-circuit the chain without touching the network.
 *
 * [headers] are the **response's** headers.
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

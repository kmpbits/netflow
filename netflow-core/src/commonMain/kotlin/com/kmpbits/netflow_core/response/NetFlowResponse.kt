package com.kmpbits.netflow_core.response

import com.kmpbits.netflow_core.alias.Header

class NetFlowResponse internal constructor(
    val code: Int,
    val headers: List<Header>,
    val body: String?,
    val errorBody: String?
) {
    val isSuccess: Boolean
        get() = code in (200..299) && body != null
}
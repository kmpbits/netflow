package com.kmpbits.netflow_core.mock

import com.kmpbits.netflow_core.enums.HttpMethod

data class NetFlowMockRequest(
    val path: String,
    val method: HttpMethod,
    val body: Map<String, Any>?,
    val headers: List<Pair<String, String>>
)

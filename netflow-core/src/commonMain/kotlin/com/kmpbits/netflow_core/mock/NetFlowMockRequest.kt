package com.kmpbits.netflow_core.mock

import com.kmpbits.netflow_core.builders.MultipartPart
import com.kmpbits.netflow_core.enums.HttpMethod

data class NetFlowMockRequest(
    val path: String,
    val method: HttpMethod,
    val body: Map<String, Any>?,
    val rawBody: String?,
    val headers: List<Pair<String, String>>,
    val parts: List<MultipartPart>? = null,
) {
    /** Case-insensitive header lookup, e.g. `request["Authorization"]`. */
    operator fun get(name: String): String? =
        headers.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
}

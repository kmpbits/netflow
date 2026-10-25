package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * Puts [payload] in `body` for 2xx responses and in `errorBody` for the rest, the same split the
 * Android engine makes, so callers read error payloads from `errorBody` on both platforms.
 */
internal fun buildNetFlowResponse(code: Int, headers: List<Header>, payload: String): NetFlowResponse =
    if (code in 200..299) {
        NetFlowResponse(code = code, headers = headers, body = payload, errorBody = null)
    } else {
        NetFlowResponse(code = code, headers = headers, body = null, errorBody = payload)
    }

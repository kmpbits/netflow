package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.request.NetFlowCall

/**
 * Builds a [NetFlowCall] — the request without a response strategy. Symmetric with
 * [NetFlowClient.call]; use it when the response side (cache, transform, paging) is composed
 * by the caller.
 */
fun NetFlowClient.prepareCall(builder: RequestBuilder.() -> Unit): NetFlowCall =
    NetFlowCall(call(builder))

package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse

internal interface HttpEngineAdapter {
    suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse
}

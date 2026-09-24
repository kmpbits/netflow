package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.flow.Flow

internal expect class InternalHttpClient {

    suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse

    fun stream(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): Flow<ByteArray>
}

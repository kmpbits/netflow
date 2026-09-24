package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.flow.Flow

internal interface HttpEngineAdapter {
    suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse

    /** Cold flow of the response body chunks, unbuffered. Non-2xx throws `HttpException`. */
    fun stream(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): Flow<ByteArray>
}

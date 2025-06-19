package com.kmpbits.netflow_core.client

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.request.NetFlowRequest

interface NetFlowClient {

    /**
     * This method should be called for every request.
     *
     * @param builder Have the customization of the request like the path and headers.
     * @return A [NetFlowRequest] that can be used to deserialize the response.
     */
    suspend fun call(builder: RequestBuilder. () -> Unit = {}): NetFlowRequest
}
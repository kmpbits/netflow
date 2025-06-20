package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

internal actual class InternalHttpClient(
    private val client: OkHttpClient
) {

    actual suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse {
        val request = requestBuilder.request
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }

        return if (response.isSuccessful) {
            NetFlowResponse(
                code = response.code,
                headers = builder.headers,
                body = response.body?.string().orEmpty(),
                errorBody = null
            )
        } else {
            NetFlowResponse(
                code = response.code,
                headers = builder.headers,
                body = null,
                errorBody = response.body?.string().orEmpty()
            )
        }
    }
}
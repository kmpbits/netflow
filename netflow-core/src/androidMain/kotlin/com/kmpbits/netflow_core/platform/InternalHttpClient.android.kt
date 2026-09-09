package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
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

        val responseHeaders = response.headers.map { (name, value) ->
            Header(HttpHeader.custom(name), value)
        }

        return if (response.isSuccessful) {
            NetFlowResponse(
                code = response.code,
                headers = responseHeaders,
                body = response.body?.string().orEmpty(),
                errorBody = null
            )
        } else {
            NetFlowResponse(
                code = response.code,
                headers = responseHeaders,
                body = null,
                errorBody = response.body?.string().orEmpty()
            )
        }
    }
}
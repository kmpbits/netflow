package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
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

    actual fun stream(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): Flow<ByteArray> =
        callbackFlow {
            val call = client.newCall(requestBuilder.request)
            launch(Dispatchers.IO) {
                try {
                    call.execute().use { response ->
                        if (!response.isSuccessful) {
                            throw HttpException(response.code, response.body?.string().orEmpty())
                        }
                        val body = response.body
                        body?.source()?.readChunks(
                            total = body.contentLength(),
                            onProgress = builder.downloadProgress,
                        ) { send(it) }
                    }
                    close()
                } catch (e: Throwable) {
                    close(e)
                }
            }
            // Collector cancelled (or channel closed): abort the call so a blocked read unblocks.
            awaitClose { call.cancel() }
        }
}
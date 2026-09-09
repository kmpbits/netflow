package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.extensions.toJson
import com.kmpbits.netflow_core.extensions.toName
import com.kmpbits.netflow_core.extensions.urlWithPath
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http.HttpMethod as OkHttpMethod

internal actual fun RequestBuilder.build(): InternalHttpRequestBuilder {
    validateMultipart()

    val requestBuilder = Request.Builder()
        .url(
            urlWithPath(
                baseUrl,
                path,
                method,
                parameters
            )
        )

    headers.forEach {
        requestBuilder.addHeader(it.first.header, it.second)
    }

    val method = method.toName()
    val requestBody = if (parts.isNotEmpty()) {
        buildMultipartRequestBody()
    } else {
        val bodyString = rawBody ?: body?.toJson()
        if (OkHttpMethod.requiresRequestBody(method) && bodyString == null) {
            throw NetFlowException("Request Body is required for $method method")
        }
        bodyString?.toRequestBody("application/json; charset=utf-8".toMediaType())
    }

    requestBuilder.method(method, requestBody)

    return InternalHttpRequestBuilder(requestBuilder)
}
package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.builders.extensions.toJsonString
import com.kmpbits.netflow_core.builders.extensions.toNSData
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.extensions.toName
import com.kmpbits.netflow_core.extensions.urlWithPath
import com.kmpbits.netflow_core.platform.InternalHttpRequestBuilder
import platform.Foundation.HTTPBody
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue

internal actual fun RequestBuilder.build(): InternalHttpRequestBuilder {
    val completeUrlString = urlWithPath(
        baseUrl = baseUrl,
        path = path,
        method = method,
        parameters = parameters
    )
    val url = NSURL(string = completeUrlString)
    val mutableRequest = NSMutableURLRequest.requestWithURL(url).apply {
        setHTTPMethod(method.toName())

        val allowsBody = method in setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)

        headers.forEach {
            setValue(it.second, forHTTPHeaderField = it.first.header)
        }

        if (body != null && allowsBody) {
            val jsonString = body!!.toJsonString()
            HTTPBody = jsonString.toNSData()
        }
    }

    return InternalHttpRequestBuilder(mutableRequest)
}
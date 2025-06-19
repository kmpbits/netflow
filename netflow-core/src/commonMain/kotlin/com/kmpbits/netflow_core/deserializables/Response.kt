package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.extensions.toModel
import com.kmpbits.netflow_core.request.NetFlowRequest

suspend inline fun <reified T> NetFlowRequest.responseToModel(): T {
    val apiCall = response()

    if (apiCall.isSuccess) {
        return apiCall.toModel()
    }

    throw HttpException(apiCall.code, apiCall.errorBody)
}
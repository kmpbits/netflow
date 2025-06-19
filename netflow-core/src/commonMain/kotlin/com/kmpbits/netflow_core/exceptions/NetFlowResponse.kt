package com.kmpbits.netflow_core.exceptions

import com.kmpbits.netflow_core.enums.ErrorResponseType
import com.kmpbits.netflow_core.response.ErrorResponse
import com.kmpbits.netflow_core.response.NetFlowResponse

fun NetFlowResponse.toError(): ErrorResponse {
    return ErrorResponse(code, errorBody, ErrorResponseType.Http)
}
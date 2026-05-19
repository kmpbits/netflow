package com.kmpbits.netflow_core.mock

data class NetFlowMockResponse(
    val code: Int = 200,
    val body: String? = null,
    val errorBody: String? = null
) {
    companion object {
        fun success(body: String? = null) = NetFlowMockResponse(code = 200, body = body)
        fun error(code: Int = 400, errorBody: String? = null) = NetFlowMockResponse(code = code, errorBody = errorBody)
        fun notFound() = NetFlowMockResponse(code = 404, errorBody = "Not found")
        fun serverError(errorBody: String? = "Internal server error") = NetFlowMockResponse(code = 500, errorBody = errorBody)
    }
}

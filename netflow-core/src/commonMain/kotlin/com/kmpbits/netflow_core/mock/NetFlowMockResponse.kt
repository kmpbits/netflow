package com.kmpbits.netflow_core.mock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class NetFlowMockResponse(
    val code: Int = 200,
    val body: String? = null,
    val errorBody: String? = null,
    val delay: Duration = 0.milliseconds
) {
    companion object {
        fun success(body: String? = null, delay: Duration = 0.milliseconds) =
            NetFlowMockResponse(code = 200, body = body, delay = delay)

        fun error(code: Int = 400, errorBody: String? = null, delay: Duration = 0.milliseconds) =
            NetFlowMockResponse(code = code, errorBody = errorBody, delay = delay)

        fun notFound(delay: Duration = 0.milliseconds) =
            NetFlowMockResponse(code = 404, errorBody = "Not found", delay = delay)

        fun serverError(errorBody: String? = "Internal server error", delay: Duration = 0.milliseconds) =
            NetFlowMockResponse(code = 500, errorBody = errorBody, delay = delay)
    }
}

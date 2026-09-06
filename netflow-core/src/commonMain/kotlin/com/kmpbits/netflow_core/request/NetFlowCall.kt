package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.response.NetFlowResponse

/**
 * A request that has been built but not yet given a response strategy. Return this from an
 * annotated API function when the caller needs to compose the response side itself — e.g. to
 * add a local cache, `onNetworkSuccess` side effects, or a `transform`.
 *
 * Finish it with any `responseX` function: `call.responseFlow { local { … } }`.
 */
class NetFlowCall internal constructor(
    /** The underlying request. Build it with [com.kmpbits.netflow_core.client.prepareCall]. */
    val request: NetFlowRequest,
) {
    suspend fun response(): NetFlowResponse = request.response()
}

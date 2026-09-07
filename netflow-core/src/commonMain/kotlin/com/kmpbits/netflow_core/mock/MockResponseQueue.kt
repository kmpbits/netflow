package com.kmpbits.netflow_core.mock

/**
 * Serves a fixed list of [NetFlowMockResponse]s across successive calls, repeating
 * the final element once exhausted. Use in a [MockNetFlowClient] handler to model
 * "401 on the first hit, 200 after the token refresh".
 */
class MockResponseQueue(vararg responses: NetFlowMockResponse) {
    private val items = responses.toList()
    private var index = 0

    fun next(): NetFlowMockResponse {
        val r = items[minOf(index, items.lastIndex)]
        index++
        return r
    }
}

package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequestBuilderBodyTest {

    @Serializable
    data class CreatePayload(val title: String, val done: Boolean)

    @Test
    fun typed_body_overload_records_rawBody_json_and_leaves_map_body_null() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("{}") }

        client.call {
            path = "todos"
            method = HttpMethod.Post
            body(CreatePayload(title = "a", done = true))
        }.response()

        val request = client.recordedRequests.single()
        assertEquals("""{"title":"a","done":true}""", request.rawBody)
        assertNull(request.body)
    }

    @Test
    fun map_body_still_uses_the_map_path_and_leaves_rawBody_null() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("{}") }

        client.call {
            path = "todos"
            method = HttpMethod.Post
            body(mapOf("title" to "a", "done" to true))
        }.response()

        val request = client.recordedRequests.single()
        assertNull(request.rawBody)
        assertEquals(mapOf("title" to "a", "done" to true), request.body)
    }
}

package com.kmpbits.sample

import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TodoApiUrlQueryHeaderMapTest {

    private val singleTodoJson = """{"userId":1,"id":9,"title":"x","completed":true}"""
    private val listJson = """[{"userId":1,"id":9,"title":"x","completed":true}]"""

    @Test
    fun generated_fetchFromUrl_uses_the_absolute_url_as_is() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(singleTodoJson) }
        val api = client.createTodoApi()

        val state = api.fetchFromUrl("https://files.example.com/export/9")

        assertIs<AsyncState.Success<*>>(state)
        val request = client.recordedRequests.single()
        assertEquals("https://files.example.com/export/9", request.path)
    }

    @Test
    fun generated_searchTodos_adds_querymap_and_headermap_entries_and_omits_nulls() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(listJson) }
        val api = client.createTodoApi()

        api.searchTodos(
            filters = mapOf("completed" to true, "skippedQuery" to null),
            extraHeaders = mapOf("X-Trace" to "abc", "skippedHeader" to null),
        )

        val request = client.recordedRequests.single()
        assertEquals("todos", request.path)
        assertEquals(listOf("completed" to true), request.parameters)
        assertEquals("abc", request["X-Trace"])
        assertNull(request["skippedHeader"])
    }

    @Test
    fun generated_searchTodos_with_null_maps_sends_neither_extra_query_nor_headers() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(listJson) }
        val api = client.createTodoApi()

        api.searchTodos(filters = null, extraHeaders = null)

        val request = client.recordedRequests.single()
        assertEquals(emptyList(), request.parameters)
    }
}

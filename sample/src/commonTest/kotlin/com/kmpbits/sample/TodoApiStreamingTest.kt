package com.kmpbits.sample

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TodoApiStreamingTest {

    @Test
    fun generated_exportTodos_streams_the_chunks() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.stream(listOf(byteArrayOf(1, 2), byteArrayOf(3)))
        }

        val out = client.createTodoApi().exportTodos().toList()

        assertEquals(2, out.size)
        assertContentEquals(byteArrayOf(3), out[1])
        client.assertCalled("todos/export", HttpMethod.Get)
    }

    @Test
    fun generated_exportTodos_throws_on_non_2xx() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        assertFailsWith<HttpException> { client.createTodoApi().exportTodos().toList() }
    }

    @Test
    fun generated_downloadTodoAttachment_binds_path_and_query() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.stream(listOf(byteArrayOf(7))) }

        client.createTodoApi().downloadTodoAttachment(todoId = 5, format = "zip").toList()

        val req = client.recordedRequests.single()
        assertEquals("todos/5/attachment", req.path)
        assertEquals(listOf("format" to "zip"), req.parameters)
    }
}

package com.kmpbits.sample

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import androidx.paging.PagingData
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.data.dto.CreateTodoRequest
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TodoApiGeneratedTest {

    private val oneTodoJson = """[{"userId":1,"id":1,"title":"a","completed":false}]"""
    private val singleTodoJson = """{"userId":1,"id":9,"title":"x","completed":true}"""

    @Test
    fun generated_getTodos_issues_GET_todos_and_deserializes_the_list() = runTest {
        val client = MockNetFlowClient { request ->
            if (request.path == "todos" && request.method == HttpMethod.Get) {
                NetFlowMockResponse.success(oneTodoJson)
            } else {
                NetFlowMockResponse.notFound()
            }
        }
        val api = client.createTodoApi()

        val state = api.getTodos(completed = null)

        client.assertCalled("todos", HttpMethod.Get)
        assertTrue(state is AsyncState.Success, "expected Success, was $state")
        assertEquals(1, (state as AsyncState.Success).data.size)
    }

    @Test
    fun generated_delete_issues_DELETE_to_the_interpolated_path() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success() }
        val api = client.createTodoApi()

        api.delete(id = 7)

        client.assertCalled("todos/7", HttpMethod.Delete)
    }

    @Test
    fun generated_create_sends_the_map_body() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(singleTodoJson) }
        val api = client.createTodoApi()

        api.create(mapOf("title" to "x", "completed" to true))

        client.assertCalled("todos", HttpMethod.Post)
        assertEquals(mapOf("title" to "x", "completed" to true), client.recordedRequests.first().body)
    }

    @Test
    fun generated_observeTodo_emits_a_success_state_from_the_flow() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(singleTodoJson) }
        val api = client.createTodoApi()

        val state = api.observeTodo(id = 9).first { it !is ResultState.Loading }

        client.assertCalled("todos/9", HttpMethod.Get)
        assertTrue(state is ResultState.Success, "expected Success, was $state")
        assertEquals(9, (state as ResultState.Success).data.id)
    }

    @Test
    fun generated_createTyped_sends_serialized_body_and_static_headers() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(singleTodoJson) }
        val api = client.createTodoApi()

        api.createTyped(CreateTodoRequest(title = "x", completed = true))

        val request = client.recordedRequests.single()
        client.assertCalled("todos", HttpMethod.Post)
        assertEquals("""{"title":"x","completed":true}""", request.rawBody)
        assertTrue(("Accept" to "application/json") in request.headers, request.headers.toString())
        assertTrue(("X-Client" to "netflow") in request.headers, request.headers.toString())
    }

    @Test
    fun generated_pagedTodos_is_callable_and_typed() {
        val client = MockNetFlowClient { NetFlowMockResponse.success("[]") }
        val api = client.createTodoApi()
        val flow: Flow<PagingData<TodoDto>> = api.pagedTodos()
        assertNotNull(flow)
    }

    @Test
    fun generated_getWrapped_unwraps_the_data_envelope() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":{"userId":1,"id":9,"title":"x","completed":true}}""")
        }
        val api = client.createTodoApi()

        val state = api.getWrapped(id = 9)

        client.assertCalled("todos/9", HttpMethod.Get)
        assertTrue(state is AsyncState.Success, "expected Success, was $state")
        assertEquals(9, (state as AsyncState.Success).data.id)
    }
}

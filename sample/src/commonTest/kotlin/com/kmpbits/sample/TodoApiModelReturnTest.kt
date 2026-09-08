package com.kmpbits.sample

import com.kmpbits.netflow_core.deserializables.responseListAsync
import com.kmpbits.netflow_core.deserializables.responseToModel
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TodoApiModelReturnTest {

    private val oneTodoJson = """{"userId":1,"id":9,"title":"x","completed":true}"""
    private val todoListJson = """[{"userId":1,"id":1,"title":"a","completed":false},{"userId":1,"id":2,"title":"b","completed":true}]"""

    @Test
    fun generated_bare_model_return_deserializes_and_returns_the_dto() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(oneTodoJson) }

        val todo = client.createTodoApi().getTodoModel(id = 9)

        assertEquals(9, todo.id)
        assertEquals("x", todo.title)
    }

    @Test
    fun generated_bare_model_return_throws_HttpException_on_error() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.notFound() }

        val e = assertFailsWith<HttpException> {
            client.createTodoApi().getTodoModel(id = 99)
        }
        assertEquals(404, e.code)
    }

    @Test
    fun generated_bare_list_model_return_deserializes_the_list() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(todoListJson) }

        val todos = client.createTodoApi().getTodoModels()

        assertEquals(2, todos.size)
        assertEquals("a", todos[0].title)
    }

    @Test
    fun generated_suspend_NetFlowCall_composes_responseAsync_and_responseToModel() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(todoListJson) }
        val api = client.createTodoApi()

        val state = api.todosCallSuspend().responseListAsync<TodoDto>()
        assertTrue(state is AsyncState.Success, "was $state")

        val list: List<TodoDto> = api.todosCallSuspend().responseToModel()
        assertEquals(2, list.size)
        client.assertCalledTimes("todos", HttpMethod.Get, times = 2)
    }
}

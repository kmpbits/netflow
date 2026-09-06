package com.kmpbits.sample

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import com.kmpbits.sample.android.data.repository.TodoApiRepositoryImpl
import com.kmpbits.sample.android.domain.model.Todo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TodoApiRepositoryImplTest {

    private val listJson = """[{"userId":1,"id":1,"title":"a","completed":false}]"""
    private val singleJson = """{"userId":1,"id":9,"title":"x","completed":true}"""

    @Test
    fun getTodos_returns_mapped_domain_models() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(listJson) }
        val repo = TodoApiRepositoryImpl(client)

        val state = repo.getTodos()

        client.assertCalled("todos", HttpMethod.Get)
        assertTrue(state is AsyncState.Success, "was $state")
        val todos: List<Todo> = (state as AsyncState.Success).data
        assertEquals(1, todos.size)
        assertEquals(Todo(userId = 1, id = 1, title = "a", completed = false), todos.first())
    }

    @Test
    fun create_serializes_body_and_returns_mapped_model() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(singleJson) }
        val repo = TodoApiRepositoryImpl(client)

        val state = repo.create(title = "x", completed = true)

        client.assertCalled("todos", HttpMethod.Post)
        assertEquals("""{"title":"x","completed":true}""", client.recordedRequests.single().rawBody)
        assertTrue(state is AsyncState.Success, "was $state")
        assertEquals(Todo(userId = 1, id = 9, title = "x", completed = true), (state as AsyncState.Success).data)
    }

    @Test
    fun observeTodo_emits_a_mapped_success_state() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success(singleJson) }
        val repo = TodoApiRepositoryImpl(client)

        val state = repo.observeTodo(id = 9).first { it !is ResultState.Loading }

        client.assertCalled("todos/9", HttpMethod.Get)
        assertTrue(state is ResultState.Success, "was $state")
        assertEquals(9, (state as ResultState.Success).data.id)
    }
}

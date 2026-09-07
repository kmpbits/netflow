package com.kmpbits.sample

import com.kmpbits.netflow_core.auth.AuthState
import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.deserializables.responseListAsync
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.dto.CreateTodoRequest
import com.kmpbits.sample.android.data.dto.TodoDto
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves a KSP-generated `@NetFlowApi` interface inherits the client's auth
 * behaviour: `createTodoApi()` passes the client straight through, so every
 * generated method bottoms out at the same `NetFlowRequest.response()` that
 * runs the 401 refresh-and-retry.
 */
class TodoApiAuthTest {

    private val oneTodoJson = """[{"userId":1,"id":1,"title":"a","completed":false}]"""

    @Test
    fun generated_method_attaches_the_bearer_token() = runTest {
        var seenAuth: String? = null
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { error("should not refresh") }
        }) { request ->
            seenAuth = request["Authorization"]
            NetFlowMockResponse.success(oneTodoJson)
        }
        val api = client.createTodoApi()

        api.getTodos(completed = null)

        assertEquals("Bearer good", seenAuth)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }

    @Test
    fun generated_method_refreshes_on_401_then_retries() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { BearerTokens("fresh", "r2") }
        }) { request ->
            if (request["Authorization"] == "Bearer fresh") NetFlowMockResponse.success(oneTodoJson)
            else NetFlowMockResponse.unauthorized()
        }
        val api = client.createTodoApi()

        val state = api.getTodos(completed = null)

        assertTrue(state is AsyncState.Success, "expected Success, was $state")
        assertEquals(1, (state as AsyncState.Success).data.size)
        client.assertCalledTimes("todos", HttpMethod.Get, times = 2)
        assertEquals(AuthState.Authenticated, client.authState.value)
    }

    @Test
    fun generated_prepareCall_method_refreshes_on_401_then_retries() = runTest {
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("expired", "r1") }
            refreshTokens { BearerTokens("fresh", "r2") }
        }) { request ->
            if (request["Authorization"] == "Bearer fresh") NetFlowMockResponse.success(oneTodoJson)
            else NetFlowMockResponse.unauthorized()
        }
        val api = client.createTodoApi()

        val state = api.todosCall().responseListAsync<TodoDto>()

        assertTrue(state is AsyncState.Success, "expected Success, was $state")
        client.assertCalledTimes("todos", HttpMethod.Get, times = 2)
    }

    @Test
    fun generated_SkipAuth_method_sends_no_token_and_does_not_refresh() = runTest {
        var seenAuth: String? = "unset"
        val client = MockNetFlowClient(auth = {
            loadTokens { BearerTokens("good", "r") }
            refreshTokens { error("should not refresh") }
        }) { request ->
            seenAuth = request["Authorization"]
            NetFlowMockResponse.unauthorized()
        }
        val api = client.createTodoApi()

        val state = api.login(CreateTodoRequest(title = "x", completed = true))

        assertTrue(state is AsyncState.Error, "expected Error, was $state")
        assertEquals(null, seenAuth)
        client.assertCalledTimes("login", HttpMethod.Post, times = 1)
    }

    @Test
    fun generated_method_on_a_client_without_auth_is_unchanged() = runTest {
        var seenAuth: String? = "unset"
        val client = MockNetFlowClient { request ->
            seenAuth = request["Authorization"]
            NetFlowMockResponse.success(oneTodoJson)
        }
        val api = client.createTodoApi()

        api.getTodos(completed = null)

        assertEquals(null, seenAuth)
        assertEquals(AuthState.Unknown, client.authState.value)
    }
}

package com.kmpbits.netflow_core.request

import com.kmpbits.netflow_core.client.prepareCall
import com.kmpbits.netflow_core.deserializables.responseAsync
import com.kmpbits.netflow_core.deserializables.responseFlow
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.netflow_core.states.ResultState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetFlowCallTest {

    @Serializable
    data class Todo(val id: Int, val title: String)

    private fun client(json: String) = MockNetFlowClient { NetFlowMockResponse.success(json) }

    @Test
    fun prepareCall_then_responseAsync_hits_the_endpoint() = runTest {
        val c = client("""{"id":1,"title":"a"}""")
        val call: NetFlowCall = c.prepareCall {
            method = HttpMethod.Get
            path = "todos/1"
        }

        val state = call.responseAsync<Todo>()

        c.assertCalled("todos/1", HttpMethod.Get)
        assertTrue(state is AsyncState.Success, "was $state")
        assertEquals(Todo(1, "a"), (state as AsyncState.Success).data)
    }

    @Test
    fun prepareCall_then_responseFlow_with_onNetworkSuccess_runs_the_hook() = runTest {
        val c = client("""{"id":2,"title":"b"}""")
        val seen = mutableListOf<Todo>()

        val state = c.prepareCall { path = "todos/2" }
            .responseFlow<Todo> { onNetworkSuccess { seen.add(it) } }
            .first { it !is ResultState.Loading }

        assertTrue(state is ResultState.Success, "was $state")
        assertEquals(listOf(Todo(2, "b")), seen)
    }
}

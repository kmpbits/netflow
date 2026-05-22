package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.ResultState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Serializable
private data class PostDto(val id: Int, val title: String)

private data class PostModel(val id: Int, val headline: String)

class FlowDeserializableTest {

    // region responseFlow — single model

    @Test
    fun `responseFlow emits Loading then Success`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"id":1,"title":"Hello"}""") }

        val states = client.call { path = "posts/1" }.responseFlow<PostDto>().toList()

        assertIs<ResultState.Loading>(states[0])
        assertIs<ResultState.Success<PostDto>>(states[1])
        assertEquals(1, (states[1] as ResultState.Success).data.id)
        assertEquals("Hello", (states[1] as ResultState.Success).data.title)
    }

    @Test
    fun `responseFlow emits Loading then Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val states = client.call { path = "posts/1" }.responseFlow<PostDto>().toList()

        assertIs<ResultState.Loading>(states[0])
        assertIs<ResultState.Error<*>>(states[1])
        assertEquals(500, (states[1] as ResultState.Error).error.code)
    }

    @Test
    fun `responseFlow emits Loading then Error on not found`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.notFound() }

        val states = client.call { path = "posts/99" }.responseFlow<PostDto>().toList()

        assertIs<ResultState.Loading>(states[0])
        assertIs<ResultState.Error<*>>(states[1])
        assertEquals(404, (states[1] as ResultState.Error).error.code)
    }

    @Test
    fun `responseFlow with transform maps ApiType to DisplayType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"id":2,"title":"World"}""") }

        val states = client.call { path = "posts/2" }.responseFlow<PostDto, PostModel>(
            transform = { PostModel(it.id, it.title.uppercase()) }
        ).toList()

        assertIs<ResultState.Success<PostModel>>(states.last())
        assertEquals("WORLD", (states.last() as ResultState.Success).data.headline)
    }

    @Test
    fun `responseFlow with wrappedResponse true deserializes data envelope`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"data":{"id":3,"title":"Wrapped"}}""") }

        val states = client.call { path = "posts/3" }.responseFlow<PostDto> { wrappedResponse = true }.toList()

        assertIs<ResultState.Success<PostDto>>(states.last())
        assertEquals("Wrapped", (states.last() as ResultState.Success).data.title)
    }

    // endregion

    // region responseListFlow

    @Test
    fun `responseListFlow emits Loading then Success with list`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""[{"id":1,"title":"A"},{"id":2,"title":"B"}]""")
        }

        val states = client.call { path = "posts" }.responseListFlow<PostDto>().toList()

        assertIs<ResultState.Loading>(states[0])
        assertIs<ResultState.Success<List<PostDto>>>(states[1])
        assertEquals(2, (states[1] as ResultState.Success).data.size)
    }

    @Test
    fun `responseListFlow with transform maps each item`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""[{"id":1,"title":"A"},{"id":2,"title":"B"}]""")
        }

        val states = client.call { path = "posts" }.responseListFlow<PostDto, PostModel>(
            transform = { PostModel(it.id, it.title.lowercase()) }
        ).toList()

        assertIs<ResultState.Success<List<PostModel>>>(states.last())
        val items = (states.last() as ResultState.Success).data
        assertEquals("a", items[0].headline)
        assertEquals("b", items[1].headline)
    }

    @Test
    fun `responseListFlow emits Loading then Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val states = client.call { path = "posts" }.responseListFlow<PostDto>().toList()

        assertIs<ResultState.Loading>(states[0])
        assertIs<ResultState.Error<*>>(states[1])
    }

    @Test
    fun `responseListFlow with wrappedResponse deserializes data list envelope`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":[{"id":1,"title":"Wrapped item"}]}""")
        }

        val states = client.call { path = "posts" }.responseListFlow<PostDto> { wrappedResponse = true }.toList()

        assertIs<ResultState.Success<List<PostDto>>>(states.last())
        assertEquals(1, (states.last() as ResultState.Success).data.size)
        assertEquals("Wrapped item", (states.last() as ResultState.Success).data[0].title)
    }

    // endregion

    // region responseWrappedFlow

    @Test
    fun `responseWrappedFlow deserializes data envelope`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"data":{"id":10,"title":"Wrapped"}}""") }

        val states = client.call { path = "posts/10" }.responseWrappedFlow<PostDto>().toList()

        assertIs<ResultState.Success<PostDto>>(states.last())
        assertEquals(10, (states.last() as ResultState.Success).data.id)
    }

    @Test
    fun `responseWrappedFlow with transform maps ApiType to DisplayType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"data":{"id":11,"title":"hello"}}""") }

        val states = client.call { path = "posts/11" }.responseWrappedFlow<PostDto, PostModel>(
            transform = { PostModel(it.id, it.title.uppercase()) }
        ).toList()

        assertIs<ResultState.Success<PostModel>>(states.last())
        assertEquals("HELLO", (states.last() as ResultState.Success).data.headline)
    }

    @Test
    fun `responseWrappedFlow returns Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val states = client.call { path = "posts/10" }.responseWrappedFlow<PostDto>().toList()

        assertIs<ResultState.Error<*>>(states.last())
        assertEquals(500, (states.last() as ResultState.Error).error.code)
    }

    // endregion

    // region responseWrappedListFlow

    @Test
    fun `responseWrappedListFlow deserializes data list envelope`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":[{"id":1,"title":"A"},{"id":2,"title":"B"}]}""")
        }

        val states = client.call { path = "posts" }.responseWrappedListFlow<PostDto>().toList()

        assertIs<ResultState.Success<List<PostDto>>>(states.last())
        assertEquals(2, (states.last() as ResultState.Success).data.size)
    }

    @Test
    fun `responseWrappedListFlow with transform maps each item`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":[{"id":1,"title":"hello"},{"id":2,"title":"world"}]}""")
        }

        val states = client.call { path = "posts" }.responseWrappedListFlow<PostDto, PostModel>(
            transform = { PostModel(it.id, it.title.uppercase()) }
        ).toList()

        assertIs<ResultState.Success<List<PostModel>>>(states.last())
        val items = (states.last() as ResultState.Success).data
        assertEquals("HELLO", items[0].headline)
        assertEquals("WORLD", items[1].headline)
    }

    @Test
    fun `responseWrappedListFlow returns Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val states = client.call { path = "posts" }.responseWrappedListFlow<PostDto>().toList()

        assertIs<ResultState.Error<*>>(states.last())
    }

    // endregion
}

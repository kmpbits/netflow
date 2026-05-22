package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.enums.ErrorResponseType
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Serializable
private data class UserDto(val id: Int, val name: String)

private data class UserModel(val id: Int, val displayName: String)

class AsyncDeserializableTest {

    // region responseAsync — single model

    @Test
    fun `responseAsync returns Success with deserialized model`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"id":1,"name":"Joel"}""") }

        val result = client.call { path = "users/1" }.responseAsync<UserDto>()

        assertIs<AsyncState.Success<UserDto>>(result)
        assertEquals(1, result.data.id)
        assertEquals("Joel", result.data.name)
    }

    @Test
    fun `responseAsync returns Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val result = client.call { path = "users/1" }.responseAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
        assertEquals(500, result.error.code)
    }

    @Test
    fun `responseAsync returns Error on not found`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.notFound() }

        val result = client.call { path = "users/99" }.responseAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
        assertEquals(404, result.error.code)
    }

    @Test
    fun `responseAsync with transform maps ApiType to DisplayType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"id":2,"name":"Alice"}""") }

        val result = client.call { path = "users/2" }.responseAsync<UserDto, UserModel>(
            transform = { UserModel(it.id, it.name.uppercase()) }
        )

        assertIs<AsyncState.Success<UserModel>>(result)
        assertEquals(2, result.data.id)
        assertEquals("ALICE", result.data.displayName)
    }

    @Test
    fun `responseAsync with wrappedResponse true deserializes data envelope`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"data":{"id":3,"name":"Bob"}}""") }

        val result = client.call { path = "users/3" }.responseAsync<UserDto> { wrappedResponse = true }

        assertIs<AsyncState.Success<UserDto>>(result)
        assertEquals(3, result.data.id)
        assertEquals("Bob", result.data.name)
    }

    @Test
    fun `responseAsync with Unit type and empty body returns Success`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success() }

        val result = client.call { path = "users/1" }.responseAsync<Unit>()

        assertIs<AsyncState.Success<Unit>>(result)
    }

    // endregion

    // region responseListAsync

    @Test
    fun `responseListAsync returns Success with deserialized list`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""[{"id":1,"name":"Joel"},{"id":2,"name":"Alice"}]""")
        }

        val result = client.call { path = "users" }.responseListAsync<UserDto>()

        assertIs<AsyncState.Success<List<UserDto>>>(result)
        assertEquals(2, result.data.size)
        assertEquals("Joel", result.data[0].name)
        assertEquals("Alice", result.data[1].name)
    }

    @Test
    fun `responseListAsync returns Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val result = client.call { path = "users" }.responseListAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
    }

    @Test
    fun `responseListAsync with transform maps each item`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""[{"id":1,"name":"Joel"},{"id":2,"name":"Alice"}]""")
        }

        val result = client.call { path = "users" }.responseListAsync<UserDto, UserModel>(
            transform = { UserModel(it.id, it.name.uppercase()) }
        )

        assertIs<AsyncState.Success<List<UserModel>>>(result)
        assertEquals("JOEL", result.data[0].displayName)
        assertEquals("ALICE", result.data[1].displayName)
    }

    @Test
    fun `responseListAsync with wrappedResponse true deserializes data envelope`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":[{"id":1,"name":"Joel"}]}""")
        }

        val result = client.call { path = "users" }.responseListAsync<UserDto> { wrappedResponse = true }

        assertIs<AsyncState.Success<List<UserDto>>>(result)
        assertEquals(1, result.data.size)
        assertEquals("Joel", result.data[0].name)
    }

    // endregion

    // region responseWrappedAsync

    @Test
    fun `responseWrappedAsync deserializes data envelope`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"data":{"id":5,"name":"Carol"}}""") }

        val result = client.call { path = "users/5" }.responseWrappedAsync<UserDto>()

        assertIs<AsyncState.Success<UserDto>>(result)
        assertEquals(5, result.data.id)
        assertEquals("Carol", result.data.name)
    }

    @Test
    fun `responseWrappedAsync with transform maps ApiType to DisplayType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"data":{"id":6,"name":"Dave"}}""") }

        val result = client.call { path = "users/6" }.responseWrappedAsync<UserDto, UserModel>(
            transform = { UserModel(it.id, it.name.lowercase()) }
        )

        assertIs<AsyncState.Success<UserModel>>(result)
        assertEquals("dave", result.data.displayName)
    }

    @Test
    fun `responseWrappedAsync returns Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val result = client.call { path = "users/5" }.responseWrappedAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
        assertEquals(500, result.error.code)
    }

    // endregion

    // region responseWrappedListAsync

    @Test
    fun `responseWrappedListAsync deserializes data list envelope`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":[{"id":1,"name":"Joel"},{"id":2,"name":"Alice"}]}""")
        }

        val result = client.call { path = "users" }.responseWrappedListAsync<UserDto>()

        assertIs<AsyncState.Success<List<UserDto>>>(result)
        assertEquals(2, result.data.size)
    }

    @Test
    fun `responseWrappedListAsync with transform maps each item`() = runTest {
        val client = MockNetFlowClient {
            NetFlowMockResponse.success("""{"data":[{"id":1,"name":"Joel"}]}""")
        }

        val result = client.call { path = "users" }.responseWrappedListAsync<UserDto, UserModel>(
            transform = { UserModel(it.id, "user_${it.id}") }
        )

        assertIs<AsyncState.Success<List<UserModel>>>(result)
        assertEquals("user_1", result.data[0].displayName)
    }

    @Test
    fun `responseWrappedListAsync returns Error on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val result = client.call { path = "users" }.responseWrappedListAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
    }

    // endregion

    // region onNetworkSuccess callback

    @Test
    fun `responseAsync invokes onNetworkSuccess with raw ApiType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"id":7,"name":"Eve"}""") }
        var captured: UserDto? = null

        client.call { path = "users/7" }.responseAsync<UserDto> {
            onNetworkSuccess { captured = it }
        }

        assertEquals(UserDto(7, "Eve"), captured)
    }

    @Test
    fun `responseAsync does not invoke onNetworkSuccess on error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }
        var invoked = false

        client.call { path = "users/7" }.responseAsync<UserDto> {
            onNetworkSuccess { invoked = true }
        }

        assertEquals(false, invoked)
    }

    // endregion

    // region error type

    @Test
    fun `responseAsync error has Http ErrorResponseType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.error(400) }

        val result = client.call { path = "users/1" }.responseAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
        assertEquals(ErrorResponseType.Http, result.error.type)
    }

    @Test
    fun `responseAsync server error has Http ErrorResponseType`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError() }

        val result = client.call { path = "users/1" }.responseAsync<UserDto>()

        assertIs<AsyncState.Error>(result)
        assertEquals(ErrorResponseType.Http, result.error.type)
    }

    // endregion
}

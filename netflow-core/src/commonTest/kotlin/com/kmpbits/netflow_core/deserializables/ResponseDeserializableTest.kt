package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
private data class CommentDto(val id: Int, val body: String)

class ResponseDeserializableTest {

    @Test
    fun `responseToModel returns deserialized model on success`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"id":1,"body":"Great post!"}""") }

        val result = client.call { path = "comments/1" }.responseToModel<CommentDto>()

        assertEquals(1, result.id)
        assertEquals("Great post!", result.body)
    }

    @Test
    fun `responseToModel throws HttpException on server error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.serverError("Internal server error") }

        assertFailsWith<HttpException> {
            client.call { path = "comments/1" }.responseToModel<CommentDto>()
        }
    }

    @Test
    fun `responseToModel throws HttpException with correct code on not found`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.notFound() }

        val ex = assertFailsWith<HttpException> {
            client.call { path = "comments/99" }.responseToModel<CommentDto>()
        }

        assertEquals(404, ex.code)
    }

    @Test
    fun `responseToModel throws HttpException with correct code on client error`() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.error(400, "Bad request") }

        val ex = assertFailsWith<HttpException> {
            client.call { path = "comments/1" }.responseToModel<CommentDto>()
        }

        assertEquals(400, ex.code)
    }
}

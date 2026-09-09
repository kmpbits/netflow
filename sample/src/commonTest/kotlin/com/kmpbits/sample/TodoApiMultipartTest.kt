package com.kmpbits.sample

import com.kmpbits.netflow_core.builders.FilePart
import com.kmpbits.netflow_core.builders.MultipartPart
import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.dto.CreateTodoRequest
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TodoApiMultipartTest {

    @Test
    fun generated_upload_sends_a_multipart_post_with_text_file_and_json_parts() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"ok":true}""") }
        val api = client.createTodoApi()

        val state = api.uploadTodoAttachment(
            todoId = 7,
            caption = "before",
            file = FilePart("shot.png", byteArrayOf(1, 2, 3, 4), "image/png"),
            meta = CreateTodoRequest(title = "t", completed = false),
        )

        assertIs<AsyncState.Success<*>>(state)
        val request = client.recordedRequests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("todos/7/attachments", request.path)

        val parts = request.parts!!
        assertEquals(3, parts.size)

        val caption = assertIs<MultipartPart.Field>(parts[0])
        assertEquals("caption", caption.name)
        assertEquals("before", caption.value)
        assertNull(caption.contentType)

        val file = assertIs<MultipartPart.File>(parts[1])
        assertEquals("file", file.name)
        assertEquals("shot.png", file.filename)
        assertEquals("image/png", file.contentType)
        assertTrue(byteArrayOf(1, 2, 3, 4).contentEquals(file.bytes))

        val meta = assertIs<MultipartPart.Field>(parts[2])
        assertEquals("meta", meta.name)
        assertEquals("application/json", meta.contentType)
        assertEquals("""{"title":"t","completed":false}""", meta.value)
    }

    @Test
    fun generated_upload_omits_a_null_serializable_part() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"ok":true}""") }
        val api = client.createTodoApi()

        api.uploadTodoAttachment(
            todoId = 1,
            caption = "c",
            file = FilePart("a.bin", byteArrayOf(0), "application/octet-stream"),
            meta = null,
        )

        val parts = client.recordedRequests.single().parts!!
        assertEquals(2, parts.size)
        assertTrue(parts.none { it.name == "meta" })
    }
}

package com.kmpbits.sample

import com.kmpbits.netflow_core.builders.FilePart
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import com.kmpbits.netflow_core.states.AsyncState
import com.kmpbits.sample.android.data.remote.createTodoApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TodoApiProgressTest {

    @Test
    fun generated_uploadTodoAttachmentWithProgress_forwards_the_callback_to_onProgress() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"ok":true}""") }
        val api = client.createTodoApi()
        var seen: Pair<Long, Long>? = null

        val state = api.uploadTodoAttachmentWithProgress(
            todoId = 1,
            file = FilePart("a.bin", byteArrayOf(1, 2, 3), "application/octet-stream"),
            onProgress = { sent, total -> seen = sent to total },
        )

        assertIs<AsyncState.Success<*>>(state)
        val callback = client.recordedRequests.single().onProgress
        assertNotNull(callback)
        callback(7, 42)
        assertEquals(7L to 42L, seen)
    }

    @Test
    fun generated_uploadTodoAttachmentWithProgress_with_null_callback_leaves_onProgress_null() = runTest {
        val client = MockNetFlowClient { NetFlowMockResponse.success("""{"ok":true}""") }
        val api = client.createTodoApi()

        api.uploadTodoAttachmentWithProgress(
            todoId = 1,
            file = FilePart("a.bin", byteArrayOf(1), "application/octet-stream"),
            onProgress = null,
        )

        assertNull(client.recordedRequests.single().onProgress)
    }
}

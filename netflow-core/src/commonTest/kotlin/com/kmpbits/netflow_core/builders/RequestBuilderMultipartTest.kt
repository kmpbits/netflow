package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.enums.HttpMethod
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.mock.MockNetFlowClient
import com.kmpbits.netflow_core.mock.NetFlowMockResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class RequestBuilderMultipartTest {

    private fun client() = MockNetFlowClient { NetFlowMockResponse.success("{}") }

    @Test
    fun multipart_block_records_field_and_file_parts_in_order() = runTest {
        val c = client()
        c.call {
            path = "upload"
            method = HttpMethod.Post
            multipart {
                part("description", "My photo")
                filePart("file", filename = "photo.jpg", bytes = byteArrayOf(1, 2, 3), contentType = "image/jpeg")
            }
        }.response()

        val parts = c.recordedRequests.single().parts
        assertEquals(2, parts?.size)
        val field = assertIs<MultipartPart.Field>(parts!![0])
        assertEquals("description", field.name)
        assertEquals("My photo", field.value)
        assertNull(field.contentType)
        val file = assertIs<MultipartPart.File>(parts[1])
        assertEquals("file", file.name)
        assertEquals("photo.jpg", file.filename)
        assertEquals("image/jpeg", file.contentType)
        assertEquals(listOf<Byte>(1, 2, 3), file.bytes.toList())
    }

    @Test
    fun part_with_explicit_content_type_is_recorded_on_the_field() = runTest {
        val c = client()
        c.call {
            path = "upload"; method = HttpMethod.Post
            multipart { part("meta", "{}", contentType = "application/json") }
        }.response()

        val field = assertIs<MultipartPart.Field>(c.recordedRequests.single().parts!!.single())
        assertEquals("application/json", field.contentType)
    }

    @Test
    fun non_multipart_request_records_null_parts() = runTest {
        val c = client()
        c.call { path = "todos"; method = HttpMethod.Get }.response()
        assertNull(c.recordedRequests.single().parts)
    }

    @Test
    fun multipart_combined_with_json_body_throws() = runTest {
        assertFailsWith<NetFlowException> {
            client().call {
                path = "upload"; method = HttpMethod.Post
                body(mapOf("a" to 1))
                multipart { part("x", "y") }
            }.response()
        }
    }

    @Test
    fun multipart_on_get_throws() = runTest {
        assertFailsWith<NetFlowException> {
            client().call {
                path = "upload"; method = HttpMethod.Get
                multipart { part("x", "y") }
            }.response()
        }
    }

    @Test
    fun multipart_with_no_parts_throws() = runTest {
        assertFailsWith<NetFlowException> {
            client().call {
                path = "upload"; method = HttpMethod.Post
                multipart { }
            }.response()
        }
    }

    @Test
    fun file_part_with_blank_filename_throws() = runTest {
        assertFailsWith<NetFlowException> {
            client().call {
                path = "upload"; method = HttpMethod.Post
                multipart { filePart("file", filename = "", bytes = byteArrayOf(1)) }
            }.response()
        }
    }

    @Test
    fun part_with_blank_name_throws() = runTest {
        assertFailsWith<NetFlowException> {
            client().call {
                path = "upload"; method = HttpMethod.Post
                multipart { part("", "y") }
            }.response()
        }
    }
}

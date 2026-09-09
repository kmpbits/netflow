package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.enums.HttpMethod
import okhttp3.MultipartBody
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestBuilderMultipartAndroidTest {

    private fun multipartRequestBuilder(): RequestBuilder =
        RequestBuilder("https://example.com", RetryBuilder(), mutableListOf()).apply {
            path = "upload"
            method = HttpMethod.Post
            multipart {
                part("description", "My photo")
                filePart("file", filename = "photo.jpg", bytes = byteArrayOf(10, 20, 30), contentType = "image/jpeg")
            }
        }

    @Test
    fun build_produces_a_multipart_form_body_with_one_part_per_entry() {
        val internal = multipartRequestBuilder().build()
        val body = internal.request.body
        assertNotNull(body)
        assertTrue(body is MultipartBody)
        assertEquals("multipart", body.contentType().type)
        assertEquals("form-data", body.contentType().subtype)
        assertEquals(2, body.size)
    }

    @Test
    fun content_type_header_carries_a_boundary() {
        val internal = multipartRequestBuilder().build()
        val contentType = (internal.request.body as MultipartBody).contentType().toString()
        assertTrue(contentType.contains("boundary="), "was: $contentType")
    }

    @Test
    fun file_part_has_content_disposition_with_filename_and_its_media_type() {
        val body = multipartRequestBuilder().build().request.body as MultipartBody
        val filePart = body.part(1)
        val disposition = filePart.headers!!["Content-Disposition"]!!
        assertTrue(disposition.contains("name=\"file\""), disposition)
        assertTrue(disposition.contains("filename=\"photo.jpg\""), disposition)
        assertEquals("image/jpeg", filePart.body.contentType().toString())
    }

    @Test
    fun field_part_body_is_the_string_value() {
        val body = multipartRequestBuilder().build().request.body as MultipartBody
        val buffer = Buffer()
        body.part(0).body.writeTo(buffer)
        assertEquals("My photo", buffer.readUtf8())
    }
}

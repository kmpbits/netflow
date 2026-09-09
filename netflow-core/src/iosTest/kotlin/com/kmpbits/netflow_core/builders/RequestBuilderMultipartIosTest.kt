package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.builders.extensions.toByteArray
import com.kmpbits.netflow_core.enums.HttpMethod
import platform.Foundation.HTTPBody
import platform.Foundation.valueForHTTPHeaderField
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestBuilderMultipartIosTest {

    private fun build() = RequestBuilder("https://example.com", RetryBuilder(), mutableListOf()).apply {
        path = "upload"
        method = HttpMethod.Post
        multipart {
            part("description", "My photo")
            filePart("file", filename = "photo.jpg", bytes = byteArrayOf(1, 2, 3), contentType = "image/jpeg")
        }
    }.build()

    @Test
    fun sets_a_non_null_http_body_containing_the_parts() {
        val request = build().request
        val body = request.HTTPBody
        assertNotNull(body)
        val text = body.toByteArray().decodeToString()
        assertTrue(text.contains("name=\"description\""), text)
        assertTrue(text.contains("filename=\"photo.jpg\""), text)
    }

    @Test
    fun sets_a_multipart_content_type_header_with_a_boundary() {
        val request = build().request
        val contentType = request.valueForHTTPHeaderField("Content-Type")
        assertNotNull(contentType)
        assertTrue(contentType.startsWith("multipart/form-data; boundary=NetFlow-"), contentType)
    }

    @Test
    fun the_boundary_in_the_header_matches_the_body_delimiter() {
        val request = build().request
        val boundary = request.valueForHTTPHeaderField("Content-Type")!!.substringAfter("boundary=")
        val text = request.HTTPBody!!.toByteArray().decodeToString()
        assertTrue(text.startsWith("--$boundary\r\n"), "body: ${text.take(80)}")
        assertTrue(text.endsWith("--$boundary--\r\n"))
    }
}

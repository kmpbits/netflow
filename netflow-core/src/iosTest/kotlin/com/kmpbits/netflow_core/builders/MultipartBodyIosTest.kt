package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.builders.extensions.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultipartBodyIosTest {

    private val boundary = "TestBoundary"

    private fun bodyText(parts: List<MultipartPart>): String =
        buildMultipartBody(parts, boundary).toByteArray().decodeToString()

    @Test
    fun single_text_field_is_framed_with_boundary_disposition_and_crlfs() {
        val text = bodyText(listOf(MultipartPart.Field("description", "My photo", contentType = null)))
        assertEquals(
            "--TestBoundary\r\n" +
                "Content-Disposition: form-data; name=\"description\"\r\n" +
                "\r\n" +
                "My photo\r\n" +
                "--TestBoundary--\r\n",
            text,
        )
    }

    @Test
    fun field_with_content_type_includes_the_content_type_header() {
        val text = bodyText(listOf(MultipartPart.Field("meta", "{}", contentType = "application/json")))
        assertTrue(
            text.startsWith(
                "--TestBoundary\r\n" +
                    "Content-Disposition: form-data; name=\"meta\"\r\n" +
                    "Content-Type: application/json\r\n" +
                    "\r\n" +
                    "{}\r\n",
            ),
            text,
        )
    }

    @Test
    fun file_part_includes_filename_and_content_type_and_raw_bytes() {
        val bytes = byteArrayOf(0, 1, 2, 3)
        val data = buildMultipartBody(
            listOf(MultipartPart.File("file", "a.bin", bytes, "application/octet-stream")),
            boundary,
        ).toByteArray()
        val text = data.decodeToString()
        assertTrue(text.contains("Content-Disposition: form-data; name=\"file\"; filename=\"a.bin\"\r\n"), text)
        assertTrue(text.contains("Content-Type: application/octet-stream\r\n"), text)
        assertTrue(text.endsWith("\r\n--TestBoundary--\r\n"), text)
    }

    @Test
    fun multiple_parts_are_concatenated_in_order() {
        val text = bodyText(
            listOf(
                MultipartPart.Field("a", "1", contentType = null),
                MultipartPart.Field("b", "2", contentType = null),
            ),
        )
        assertEquals(
            "--TestBoundary\r\n" +
                "Content-Disposition: form-data; name=\"a\"\r\n\r\n1\r\n" +
                "--TestBoundary\r\n" +
                "Content-Disposition: form-data; name=\"b\"\r\n\r\n2\r\n" +
                "--TestBoundary--\r\n",
            text,
        )
    }

    @Test
    fun multipartBoundary_is_prefixed_and_unique() {
        val a = multipartBoundary()
        val b = multipartBoundary()
        assertTrue(a.startsWith("NetFlow-"), a)
        assertTrue(a != b)
    }
}

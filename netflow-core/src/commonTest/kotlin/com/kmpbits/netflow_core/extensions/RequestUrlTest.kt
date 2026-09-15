package com.kmpbits.netflow_core.extensions

import com.kmpbits.netflow_core.alias.Parameter
import com.kmpbits.netflow_core.enums.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestUrlTest {

    @Test
    fun createUrl_joins_a_relative_path_to_the_base_url() {
        assertEquals("https://api.example.com/todos", createUrl("https://api.example.com", "todos"))
        assertEquals("https://api.example.com/todos", createUrl("https://api.example.com/", "/todos"))
    }

    @Test
    fun createUrl_returns_an_absolute_http_or_https_path_unchanged_ignoring_base_url() {
        assertEquals(
            "https://other.host/x",
            createUrl("https://api.example.com", "https://other.host/x"),
        )
        assertEquals(
            "http://other.host/x",
            createUrl("https://api.example.com", "http://other.host/x"),
        )
    }

    @Test
    fun createUrl_with_empty_relative_path_returns_the_bare_base_url() {
        assertEquals("https://api.example.com", createUrl("https://api.example.com", ""))
    }

    @Test
    fun urlWithPath_appends_the_query_string_ahead_of_an_absolute_path() {
        val result = urlWithPath(
            baseUrl = "https://api.example.com",
            path = "https://other.host/x",
            method = HttpMethod.Get,
            parameters = mutableListOf(Parameter("page", 2)),
        )
        assertEquals("https://other.host/x?page=2", result)
    }
}

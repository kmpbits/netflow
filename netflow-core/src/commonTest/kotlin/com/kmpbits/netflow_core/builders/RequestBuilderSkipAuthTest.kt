package com.kmpbits.netflow_core.builders

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RequestBuilderSkipAuthTest {

    @Test
    fun `skipAuth defaults to false`() {
        val builder = RequestBuilder("https://x", RetryBuilder(), mutableListOf())
        assertFalse(builder.skipAuth)
    }

    @Test
    fun `skipAuth sets the flag`() {
        val builder = RequestBuilder("https://x", RetryBuilder(), mutableListOf()).apply { skipAuth() }
        assertTrue(builder.skipAuth)
    }
}

package com.kmpbits.netflow_core.pinning

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostMatcherTest {

    @Test
    fun `an exact pattern only matches the host itself`() {
        assertTrue(matchesPattern("api.example.com", "api.example.com"))
        assertFalse(matchesPattern("api.example.com", "www.api.example.com"))
        assertFalse(matchesPattern("api.example.com", "example.com"))
    }

    @Test
    fun `an exact pattern ignores case`() {
        assertTrue(matchesPattern("API.Example.COM", "api.example.com"))
    }

    @Test
    fun `wildcard matches exactly one subdomain level`() {
        assertTrue(matchesPattern("*.example.com", "api.example.com"))
        assertFalse(matchesPattern("*.example.com", "a.b.example.com"))
    }

    @Test
    fun `wildcard does not match the base domain`() {
        assertFalse(matchesPattern("*.example.com", "example.com"))
    }

    @Test
    fun `wildcard does not match a different domain`() {
        assertFalse(matchesPattern("*.example.com", "api.other.com"))
    }
}

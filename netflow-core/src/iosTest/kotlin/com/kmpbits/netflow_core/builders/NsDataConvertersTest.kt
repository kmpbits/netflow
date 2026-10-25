package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.builders.extensions.toByteArray
import platform.Foundation.NSData
import kotlin.test.Test
import kotlin.test.assertEquals

class NsDataConvertersTest {

    @Test
    fun an_empty_nsdata_becomes_an_empty_byte_array() {
        assertEquals(0, NSData().toByteArray().size)
    }
}

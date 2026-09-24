package com.kmpbits.netflow_core.platform

import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferedSourceChunksTest {

    @Test
    fun `splits the source into chunks no larger than chunkSize`() = runTest {
        val source = Buffer().write(ByteArray(10) { it.toByte() })
        val chunks = mutableListOf<ByteArray>()

        source.readChunks(chunkSize = 4) { chunks += it }

        assertEquals(listOf(4, 4, 2), chunks.map { it.size })
        assertContentEquals(ByteArray(10) { it.toByte() }, chunks.reduce { a, b -> a + b })
    }

    @Test
    fun `an empty source emits nothing`() = runTest {
        val chunks = mutableListOf<ByteArray>()

        Buffer().readChunks { chunks += it }

        assertTrue(chunks.isEmpty())
    }
}

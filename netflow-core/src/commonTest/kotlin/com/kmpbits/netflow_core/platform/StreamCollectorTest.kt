package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.exceptions.HttpException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StreamCollectorTest {

    private fun collector(
        capacity: Int = 64,
        onSuspend: () -> Unit = {},
        onResume: () -> Unit = {},
    ) = StreamCollector(capacity, onSuspend, onResume)

    @Test
    fun `emits chunks in order then completes`() = runTest {
        val c = collector()
        c.onResponse(200)
        c.onData(byteArrayOf(1, 2))
        c.onData(byteArrayOf(3))
        c.onComplete(null)

        val chunks = c.flow.toList()

        assertEquals(2, chunks.size)
        assertContentEquals(byteArrayOf(1, 2), chunks[0])
        assertContentEquals(byteArrayOf(3), chunks[1])
    }

    @Test
    fun `non-2xx throws HttpException carrying the accumulated error body`() = runTest {
        val c = collector()
        c.onResponse(404)
        c.onData("not ".encodeToByteArray())
        c.onData("found".encodeToByteArray())
        c.onComplete(null)

        val e = assertFailsWith<HttpException> { c.flow.toList() }

        assertEquals(404, e.code)
        assertEquals("not found", e.message)
    }

    @Test
    fun `non-2xx never emits the error body as a chunk`() = runTest {
        val c = collector()
        c.onResponse(500)
        c.onData(byteArrayOf(9))
        c.onComplete(null)

        val seen = mutableListOf<ByteArray>()
        assertFailsWith<HttpException> { c.flow.collect { seen += it } }

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `mid-stream failure is delivered after the chunks already received`() = runTest {
        val c = collector()
        c.onResponse(200)
        c.onData(byteArrayOf(1))
        c.onComplete(IllegalStateException("boom"))

        val seen = mutableListOf<ByteArray>()
        val e = assertFailsWith<IllegalStateException> { c.flow.collect { seen += it } }

        assertEquals("boom", e.message)
        assertEquals(1, seen.size)
    }

    @Test
    fun `suspends once when capacity is reached and resumes once when drained`() = runTest {
        var suspends = 0
        var resumes = 0
        val c = collector(capacity = 4, onSuspend = { suspends++ }, onResume = { resumes++ })
        c.onResponse(200)

        repeat(6) { c.onData(byteArrayOf(it.toByte())) } // hits capacity at the 4th, stays suspended
        assertEquals(1, suspends)
        assertEquals(0, resumes)

        c.onComplete(null)
        c.flow.toList()

        assertEquals(1, suspends)
        assertEquals(1, resumes)
    }

    @Test
    fun `does not suspend below capacity`() = runTest {
        var suspends = 0
        val c = collector(capacity = 4, onSuspend = { suspends++ })
        c.onResponse(200)
        repeat(3) { c.onData(byteArrayOf(1)) }
        c.onComplete(null)
        c.flow.toList()

        assertEquals(0, suspends)
    }

    @Test
    fun `reports cumulative progress with the content length`() = runTest {
        val seen = mutableListOf<Pair<Long, Long>>()
        val c = StreamCollector(onSuspend = {}, onResume = {}, onProgress = { r, t -> seen += r to t })
        c.onResponse(200, contentLength = 10)
        c.onData(byteArrayOf(1, 2, 3))
        c.onData(byteArrayOf(4, 5))
        c.onComplete(null)
        c.flow.toList()

        assertEquals(listOf(3L to 10L, 5L to 10L), seen)
    }

    @Test
    fun `an unknown content length is reported as minus one`() = runTest {
        val seen = mutableListOf<Pair<Long, Long>>()
        val c = StreamCollector(onSuspend = {}, onResume = {}, onProgress = { r, t -> seen += r to t })
        c.onResponse(200)
        c.onData(byteArrayOf(1))
        c.onComplete(null)
        c.flow.toList()

        assertEquals(listOf(1L to -1L), seen)
    }

    @Test
    fun `no progress is reported for a non-2xx body`() = runTest {
        val seen = mutableListOf<Pair<Long, Long>>()
        val c = StreamCollector(onSuspend = {}, onResume = {}, onProgress = { r, t -> seen += r to t })
        c.onResponse(500, contentLength = 4)
        c.onData(byteArrayOf(1, 2, 3, 4))
        c.onComplete(null)
        assertFailsWith<HttpException> { c.flow.toList() }

        assertTrue(seen.isEmpty())
    }
}

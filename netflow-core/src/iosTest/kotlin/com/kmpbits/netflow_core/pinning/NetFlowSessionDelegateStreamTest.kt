package com.kmpbits.netflow_core.pinning

import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.platform.StreamCollector
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.create
import platform.Foundation.dataTaskWithURL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalForeignApi::class)
class NetFlowSessionDelegateStreamTest {

    private fun task(): NSURLSessionDataTask =
        NSURLSession.sharedSession().dataTaskWithURL(NSURL(string = "https://example.com")!!)

    private fun data(bytes: ByteArray): NSData =
        bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }

    private fun delegate() = NetFlowSessionDelegate(pinning = null, followRedirects = false)

    @Test
    fun didReceiveData_and_didComplete_route_to_the_registered_collector() = runTest {
        val delegate = delegate()
        val task = task()
        val collector = StreamCollector(onSuspend = {}, onResume = {})
        delegate.registerStream(task, collector)

        delegate.URLSession(NSURLSession.sharedSession(), task, didReceiveData = data(byteArrayOf(1, 2, 3)))
        delegate.URLSession(NSURLSession.sharedSession(), task, didCompleteWithError = null)

        val chunks = collector.flow.toList()
        assertEquals(1, chunks.size)
        assertContentEquals(byteArrayOf(1, 2, 3), chunks[0])
    }

    @Test
    fun a_transport_error_reaches_the_flow() = runTest {
        val delegate = delegate()
        val task = task()
        val collector = StreamCollector(onSuspend = {}, onResume = {})
        delegate.registerStream(task, collector)

        delegate.URLSession(
            NSURLSession.sharedSession(),
            task,
            didCompleteWithError = NSError.errorWithDomain("test", -1, null),
        )

        assertFailsWith<HttpException> { collector.flow.toList() }
    }

    @Test
    fun data_for_an_unregistered_task_is_ignored() {
        delegate().URLSession(NSURLSession.sharedSession(), task(), didReceiveData = data(byteArrayOf(1)))
    }
}

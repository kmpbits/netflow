package com.kmpbits.netflow_core.pinning

import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithURL
import kotlin.test.Test
import kotlin.test.assertEquals

class NetFlowSessionDelegateProgressTest {

    private fun task(): NSURLSessionDataTask =
        NSURLSession.sharedSession().dataTaskWithURL(NSURL(string = "https://example.com")!!)

    @Test
    fun didSendBodyData_routes_to_the_registered_task_callback() {
        val delegate = NetFlowSessionDelegate(pinning = null, followRedirects = false)
        val task = task()
        var seenSent = 0L
        var seenTotal = 0L
        delegate.registerProgress(task) { sent, total -> seenSent = sent; seenTotal = total }

        delegate.URLSession(
            session = NSURLSession.sharedSession(),
            task = task,
            didSendBodyData = 10,
            totalBytesSent = 40,
            totalBytesExpectedToSend = 100,
        )

        assertEquals(40L, seenSent)
        assertEquals(100L, seenTotal)
    }

    @Test
    fun a_task_with_no_registered_callback_is_silently_ignored() {
        val delegate = NetFlowSessionDelegate(pinning = null, followRedirects = false)

        // No registerProgress call — must not throw.
        delegate.URLSession(
            session = NSURLSession.sharedSession(),
            task = task(),
            didSendBodyData = 10,
            totalBytesSent = 10,
            totalBytesExpectedToSend = 10,
        )
    }

    @Test
    fun clearProgress_removes_the_callback_so_further_calls_are_ignored() {
        val delegate = NetFlowSessionDelegate(pinning = null, followRedirects = false)
        val task = task()
        var callCount = 0
        delegate.registerProgress(task) { _, _ -> callCount++ }
        delegate.clearProgress(task)

        delegate.URLSession(
            session = NSURLSession.sharedSession(),
            task = task,
            didSendBodyData = 1,
            totalBytesSent = 1,
            totalBytesExpectedToSend = 1,
        )

        assertEquals(0, callCount)
    }
}

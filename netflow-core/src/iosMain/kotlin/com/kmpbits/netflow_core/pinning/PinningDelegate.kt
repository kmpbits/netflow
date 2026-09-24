package com.kmpbits.netflow_core.pinning

import com.kmpbits.netflow_core.builders.extensions.toByteArray
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.platform.StreamCollector
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSLock
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.NSURLSessionResponseAllow
import platform.Foundation.NSURLSessionResponseDisposition
import platform.Foundation.NSURLSessionTask
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateRef
import platform.Security.SecTrustCopyCertificateChain
import platform.Security.SecTrustEvaluateWithError
import platform.darwin.NSObject

/**
 * The session's delegate. Checks SPKI pins **after** normal chain validation —
 * pinning is additive, never a substitute: skipping `SecTrustEvaluateWithError`
 * would turn this into a security downgrade.
 *
 * Hosts with no pin declared follow the system's default handling.
 *
 * Note: `NSURLSession` retains the delegate strongly until `invalidateAndCancel()`.
 * Since the client typically lives for the app's lifetime, this is acceptable.
 */
@OptIn(ExperimentalForeignApi::class)
internal class NetFlowSessionDelegate(
    private val pinning: PinningConfig?,
    private val followRedirects: Boolean,
) : NSObject(),
    platform.Foundation.NSURLSessionDelegateProtocol,
    platform.Foundation.NSURLSessionTaskDelegateProtocol,
    platform.Foundation.NSURLSessionDataDelegateProtocol {

    private val progressCallbacks = mutableMapOf<NSURLSessionTask, (Long, Long) -> Unit>()

    internal fun registerProgress(task: NSURLSessionTask, callback: (Long, Long) -> Unit) {
        progressCallbacks[task] = callback
    }

    internal fun clearProgress(task: NSURLSessionTask) {
        progressCallbacks.remove(task)
    }

    // The map is touched from the caller's thread (register/clear) and the delegate queue (callbacks).
    private val streamLock = NSLock()
    private val streamCollectors = mutableMapOf<NSURLSessionTask, StreamCollector>()

    internal fun registerStream(task: NSURLSessionTask, collector: StreamCollector) {
        streamLock.lock()
        try { streamCollectors[task] = collector } finally { streamLock.unlock() }
    }

    internal fun clearStream(task: NSURLSessionTask) {
        streamLock.lock()
        try { streamCollectors.remove(task) } finally { streamLock.unlock() }
    }

    private fun collectorFor(task: NSURLSessionTask): StreamCollector? {
        streamLock.lock()
        try { return streamCollectors[task] } finally { streamLock.unlock() }
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: NSURLSessionDataTask,
        didReceiveResponse: NSURLResponse,
        completionHandler: (NSURLSessionResponseDisposition) -> Unit,
    ) {
        val status = (didReceiveResponse as? NSHTTPURLResponse)?.statusCode?.toInt()
        // expectedContentLength is -1 (NSURLResponseUnknownLength) when the server sent none.
        if (status != null) collectorFor(dataTask)?.onResponse(status, didReceiveResponse.expectedContentLength)
        // Must be called for every data task the session delegate sees, streaming or not.
        completionHandler(NSURLSessionResponseAllow)
    }

    override fun URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData: NSData) {
        collectorFor(dataTask)?.onData(didReceiveData.toByteArray())
    }

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
        collectorFor(task)?.onComplete(
            didCompleteWithError?.let { HttpException(it.code.convert(), it.localizedDescription) }
        )
    }

    override fun URLSession(
        session: NSURLSession,
        didReceiveChallenge: NSURLAuthenticationChallenge,
        completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit
    ) {
        val space = didReceiveChallenge.protectionSpace

        if (space.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            return
        }

        val config = pinning
        val expected = config?.hashesFor(space.host).orEmpty()
        if (expected.isEmpty()) {
            completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
            return
        }

        val trust = space.serverTrust
        if (trust == null) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            return
        }

        // 1. Normal chain validation, first and always.
        if (!SecTrustEvaluateWithError(trust, null)) {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            return
        }

        // 2. Only then, the pin.
        val chain = SecTrustCopyCertificateChain(trust)
        val matched = if (chain == null) {
            false
        } else {
            val count = CFArrayGetCount(chain)
            var found = false
            var i = 0L
            while (i < count && !found) {
                @Suppress("UNCHECKED_CAST")
                val cert = CFArrayGetValueAtIndex(chain, i) as SecCertificateRef?
                val hash = cert?.let { spkiSha256(it) }
                if (hash != null && expected.contains(hash)) found = true
                i++
            }
            found
        }
        chain?.let { CFRelease(it) }

        if (matched) {
            completionHandler(
                NSURLSessionAuthChallengeUseCredential,
                NSURLCredential.credentialForTrust(trust)
            )
        } else {
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        }
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didSendBodyData: Long,
        totalBytesSent: Long,
        totalBytesExpectedToSend: Long,
    ) {
        progressCallbacks[task]?.invoke(totalBytesSent, totalBytesExpectedToSend)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        willPerformHTTPRedirection: NSHTTPURLResponse,
        newRequest: NSURLRequest,
        completionHandler: (NSURLRequest?) -> Unit
    ) {
        // null blocks the redirect; returning newRequest follows it.
        completionHandler(if (followRedirects) newRequest else null)
    }
}

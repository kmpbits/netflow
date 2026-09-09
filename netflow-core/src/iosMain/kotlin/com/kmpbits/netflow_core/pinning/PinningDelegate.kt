package com.kmpbits.netflow_core.pinning

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
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
    platform.Foundation.NSURLSessionTaskDelegateProtocol {

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
        willPerformHTTPRedirection: NSHTTPURLResponse,
        newRequest: NSURLRequest,
        completionHandler: (NSURLRequest?) -> Unit
    ) {
        // null blocks the redirect; returning newRequest follows it.
        completionHandler(if (followRedirects) newRequest else null)
    }
}

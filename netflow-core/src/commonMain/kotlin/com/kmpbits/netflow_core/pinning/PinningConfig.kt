package com.kmpbits.netflow_core.pinning

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_core.exceptions.NetFlowException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal data class Pin(val pattern: String, val hashes: List<String>)

/**
 * Certificate pinning by SHA-256 hash of the public key (SPKI).
 *
 * ```kotlin
 * netflowClient {
 *     pinning {
 *         pin("api.example.com", "sha256/AAAA…=", "sha256/BBBB…=")
 *         pin("*.example.com", "sha256/CCCC…=")
 *     }
 * }
 * ```
 *
 * Hosts with no pin declared are unaffected — they keep the system's normal
 * validation. A pin failure fails the request; there is no report-only mode.
 *
 * **Always declare a backup pin** for the next key: without one, certificate
 * rotation leaves installed apps unable to connect.
 */
@NetFlowMarker
class PinningConfig internal constructor() {

    internal val pins: MutableList<Pin> = mutableListOf()

    /**
     * @param host `api.example.com` or `*.example.com` (exactly one subdomain
     * level). OkHttp's `**.` pattern is not supported.
     * @param sha256Hashes one or more hashes in the form `sha256/<base64 of 32 bytes>`.
     */
    fun pin(host: String, vararg sha256Hashes: String) {
        pins.add(Pin(host, sha256Hashes.toList()))
    }
}

private const val PREFIX = "sha256/"

/** Fails early, at client construction time, instead of on the first request. */
@OptIn(ExperimentalEncodingApi::class)
internal fun PinningConfig.validate() {
    pins.forEach { pin ->
        if (pin.pattern.isBlank()) {
            throw NetFlowException("Pinning: host must not be blank")
        }
        if (pin.hashes.isEmpty()) {
            throw NetFlowException("Pinning: '${pin.pattern}' has no pin declared")
        }
        pin.hashes.forEach { hash ->
            if (!hash.startsWith(PREFIX)) {
                throw NetFlowException("Pinning: '$hash' must start with '$PREFIX'")
            }
            val decoded = try {
                Base64.decode(hash.removePrefix(PREFIX))
            } catch (e: IllegalArgumentException) {
                throw NetFlowException("Pinning: '$hash' is not valid base64")
            }
            if (decoded.size != 32) {
                throw NetFlowException("Pinning: '$hash' decodes to ${decoded.size} bytes; a SHA-256 has 32")
            }
        }
    }
}

/**
 * Semantics replicated from OkHttp's `CertificatePinner`: `*.example.com` matches
 * exactly one subdomain level, and not the base domain.
 */
internal fun matchesPattern(pattern: String, host: String): Boolean {
    if (!pattern.startsWith("*.")) return pattern.equals(host, ignoreCase = true)

    val suffix = pattern.substring(1) // ".example.com"
    if (!host.endsWith(suffix, ignoreCase = true)) return false
    if (host.length <= suffix.length) return false

    val label = host.dropLast(suffix.length)
    return label.isNotEmpty() && !label.contains('.')
}

/** All hashes declared for [host], across every matching pattern. Empty = host not pinned. */
internal fun PinningConfig.hashesFor(host: String): List<String> =
    pins.filter { matchesPattern(it.pattern, host) }.flatMap { it.hashes }

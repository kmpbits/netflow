package com.kmpbits.netflow_core.pinning

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFNumberIntType
import platform.Security.SecCertificateCopyKey
import platform.Security.SecCertificateRef
import platform.Security.SecKeyCopyAttributes
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import platform.Security.kSecAttrKeyTypeRSA
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * ASN.1 SubjectPublicKeyInfo headers. `SecKeyCopyExternalRepresentation` returns
 * the raw key; to get the same hash that `openssl` and OkHttp's
 * `CertificatePinner` produce, the key-type header has to be prefixed before
 * hashing with SHA-256.
 */
private val RSA_2048_HEADER = byteArrayOf(
    0x30, 0x82.toByte(), 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48,
    0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03,
    0x82.toByte(), 0x01, 0x0f, 0x00
)

private val RSA_4096_HEADER = byteArrayOf(
    0x30, 0x82.toByte(), 0x02, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86.toByte(), 0x48,
    0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03,
    0x82.toByte(), 0x02, 0x0f, 0x00
)

private val EC_P256_HEADER = byteArrayOf(
    0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d,
    0x02, 0x01, 0x06, 0x08, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x03, 0x01,
    0x07, 0x03, 0x42, 0x00
)

private val EC_P384_HEADER = byteArrayOf(
    0x30, 0x76, 0x30, 0x10, 0x06, 0x07, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d,
    0x02, 0x01, 0x06, 0x05, 0x2b, 0x81.toByte(), 0x04, 0x00, 0x22, 0x03, 0x62, 0x00
)

/**
 * The SPKI pin of [certificate], in the form `sha256/<base64>`.
 *
 * Returns `null` when the key's type or size is unsupported — callers treat
 * `null` as a pin failure (fail closed), never as success.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
internal fun spkiSha256(certificate: SecCertificateRef): String? {
    val key = SecCertificateCopyKey(certificate) ?: return null

    try {
        val attributes = SecKeyCopyAttributes(key) ?: return null
        val keyType = CFDictionaryGetValue(attributes, kSecAttrKeyType)
        val keySizeRef = CFDictionaryGetValue(attributes, kSecAttrKeySizeInBits)
        val keySize = cfNumberToInt(keySizeRef) ?: return null

        // Security framework singleton constants: pointer identity is enough to
        // compare them, no need for CFEqual.
        val header = when {
            keyType == kSecAttrKeyTypeRSA && keySize == 2048 -> RSA_2048_HEADER
            keyType == kSecAttrKeyTypeRSA && keySize == 4096 -> RSA_4096_HEADER
            keyType == kSecAttrKeyTypeECSECPrimeRandom && keySize == 256 -> EC_P256_HEADER
            keyType == kSecAttrKeyTypeECSECPrimeRandom && keySize == 384 -> EC_P384_HEADER
            else -> null
        }
        CFRelease(attributes)
        if (header == null) return null

        val rawKey = SecKeyCopyExternalRepresentation(key, null) ?: return null
        val keyBytes = cfDataToByteArray(rawKey)
        CFRelease(rawKey)

        val spki = header + keyBytes
        return "sha256/" + Base64.encode(sha256(spki))
    } finally {
        CFRelease(key)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun sha256(bytes: ByteArray): ByteArray {
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { input ->
        digest.usePinned { output ->
            CC_SHA256(input.addressOf(0), bytes.size.toUInt(), output.addressOf(0).reinterpret())
        }
    }
    return digest
}

@OptIn(ExperimentalForeignApi::class)
private fun cfNumberToInt(value: CFTypeRef?): Int? {
    if (value == null) return null

    val out = IntArray(1)
    val ok = out.usePinned { pinned ->
        CFNumberGetValue(value.reinterpret(), kCFNumberIntType, pinned.addressOf(0))
    }
    return if (ok) out[0] else null
}

@OptIn(ExperimentalForeignApi::class)
private fun cfDataToByteArray(data: CFDataRef): ByteArray {
    val length = CFDataGetLength(data).toInt()
    if (length == 0) return ByteArray(0)

    val pointer = CFDataGetBytePtr(data) ?: return ByteArray(0)
    return ByteArray(length) { index -> pointer[index].toByte() }
}

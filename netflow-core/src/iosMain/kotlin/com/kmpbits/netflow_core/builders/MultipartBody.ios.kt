package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.builders.extensions.toNSData
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSUUID
import platform.Foundation.appendData
import platform.Foundation.create

internal fun multipartBoundary(): String = "NetFlow-" + NSUUID().UUIDString

/**
 * Assembles a `multipart/form-data` body by hand: for each part a `--boundary`
 * delimiter, a `Content-Disposition` (and optional `Content-Type`) header block,
 * a blank line, the payload bytes, and a trailing CRLF; then the closing
 * `--boundary--` delimiter.
 */
@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal fun buildMultipartBody(parts: List<MultipartPart>, boundary: String): NSData {
    val data = NSMutableData()

    fun appendString(s: String) = data.appendData(s.toNSData())
    fun appendBytes(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        bytes.usePinned { pinned ->
            data.appendData(NSData.create(bytes = pinned.addressOf(0), length = bytes.size.convert()))
        }
    }

    parts.forEach { part ->
        appendString("--$boundary\r\n")
        when (part) {
            is MultipartPart.Field -> {
                appendString("Content-Disposition: form-data; name=\"${part.name}\"\r\n")
                if (part.contentType != null) {
                    appendString("Content-Type: ${part.contentType}\r\n")
                }
                appendString("\r\n")
                appendString(part.value)
            }

            is MultipartPart.File -> {
                appendString(
                    "Content-Disposition: form-data; name=\"${part.name}\"; filename=\"${part.filename}\"\r\n",
                )
                appendString("Content-Type: ${part.contentType}\r\n")
                appendString("\r\n")
                appendBytes(part.bytes)
            }
        }
        appendString("\r\n")
    }
    appendString("--$boundary--\r\n")

    return data
}

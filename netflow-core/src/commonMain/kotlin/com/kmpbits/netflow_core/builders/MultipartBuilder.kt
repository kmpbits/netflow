package com.kmpbits.netflow_core.builders

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_core.exceptions.NetFlowException
import com.kmpbits.netflow_core.extensions.json
import kotlinx.serialization.encodeToString

/**
 * Receiver of the [multipart] block. Collects text ([part]) and file ([filePart])
 * parts, in call order, for a `multipart/form-data` request body.
 */
@NetFlowMarker
class MultipartBuilder internal constructor() {

    internal val parts: MutableList<MultipartPart> = mutableListOf()

    internal var progressCallback: ((sent: Long, total: Long) -> Unit)? = null
        private set

    /**
     * Reports upload byte progress for this multipart body. Fires on whatever thread the
     * platform delivers it on (OkHttp's write thread on Android, the session's private
     * delegate queue on iOS) — hop to `Dispatchers.Main` yourself if updating UI.
     */
    fun onProgress(callback: (sent: Long, total: Long) -> Unit) {
        progressCallback = callback
    }

    /** A text form field. [value] is sent as `value.toString()`, no `Content-Type`. */
    fun part(name: String, value: Any) {
        requireName(name)
        parts.add(MultipartPart.Field(name, value.toString(), contentType = null))
    }

    /** A text form field with an explicit [contentType] (e.g. `application/json`). */
    fun part(name: String, value: Any, contentType: String) {
        requireName(name)
        parts.add(MultipartPart.Field(name, value.toString(), contentType))
    }

    /** A file field from a prepared [FilePart]. */
    fun filePart(name: String, part: FilePart) {
        filePart(name, part.filename, part.bytes, part.contentType)
    }

    /** A file field from raw [bytes]. */
    fun filePart(
        name: String,
        filename: String,
        bytes: ByteArray,
        contentType: String = "application/octet-stream",
    ) {
        requireName(name)
        if (filename.isBlank()) {
            throw NetFlowException("File part '$name' requires a non-empty filename")
        }
        parts.add(MultipartPart.File(name, filename, bytes, contentType))
    }

    /**
     * A text field carrying [value] serialized to JSON (`Content-Type: application/json`).
     * Public inline so KSP-generated code can call it without touching NetFlow's
     * `@PublishedApi internal` `json()`.
     */
    inline fun <reified T> jsonPart(name: String, value: T) {
        part(name, json().encodeToString(value), "application/json")
    }

    private fun requireName(name: String) {
        if (name.isBlank()) throw NetFlowException("Multipart part name must not be empty")
    }
}

/**
 * Send this request as `multipart/form-data`. Use with `POST`, `PUT` or `PATCH`.
 * Cannot be combined with `body(...)`.
 *
 * ```
 * client.call {
 *     POST("/upload")
 *     multipart {
 *         part("description", "My photo")
 *         filePart("file", filename = "photo.jpg", bytes = imageBytes, contentType = "image/jpeg")
 *     }
 * }
 * ```
 */
fun RequestBuilder.multipart(block: MultipartBuilder.() -> Unit) {
    val builder = MultipartBuilder().apply(block)
    if (builder.parts.isEmpty()) throw NetFlowException("Multipart body has no parts")
    parts.clear()
    parts.addAll(builder.parts)
    onProgress = builder.progressCallback
}

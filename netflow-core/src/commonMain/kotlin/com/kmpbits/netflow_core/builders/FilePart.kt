package com.kmpbits.netflow_core.builders

/**
 * A file to send as one part of a [multipart] request body. Bytes are held in
 * memory — suitable for images and small-to-medium uploads.
 *
 * @param filename the name reported to the server in `Content-Disposition`.
 * @param bytes the file content.
 * @param contentType the part's `Content-Type`; defaults to `application/octet-stream`.
 */
class FilePart(
    val filename: String,
    val bytes: ByteArray,
    val contentType: String = "application/octet-stream",
)

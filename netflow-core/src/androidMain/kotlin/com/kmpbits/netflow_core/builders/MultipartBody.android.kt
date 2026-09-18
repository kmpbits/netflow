package com.kmpbits.netflow_core.builders

import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer

/** Builds an OkHttp [MultipartBody] from this request's collected [RequestBuilder.parts]. */
internal fun RequestBuilder.buildMultipartRequestBody(): RequestBody {
    val builder = MultipartBody.Builder()
        .setType("multipart/$multipartSubtype".toMediaTypeOrNull() ?: MultipartBody.FORM)

    parts.forEach { part ->
        when (part) {
            is MultipartPart.Field -> {
                val mediaType = part.contentType?.toMediaTypeOrNull()
                if (mediaType == null) {
                    builder.addFormDataPart(part.name, part.value)
                } else {
                    builder.addPart(
                        Headers.headersOf(
                            "Content-Disposition", "form-data; name=\"${part.name}\"",
                        ),
                        part.value.toRequestBody(mediaType),
                    )
                }
            }

            is MultipartPart.File -> builder.addFormDataPart(
                part.name,
                part.filename,
                part.bytes.toRequestBody(part.contentType.toMediaTypeOrNull()),
            )
        }
    }

    val body = builder.build()
    val progress = onProgress
    return if (progress != null) CountingRequestBody(body, progress) else body
}

/** Wraps [delegate], reporting cumulative bytes written to [onProgress] as OkHttp writes it. */
private class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (sent: Long, total: Long) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        var bytesWritten = 0L
        val total = contentLength()
        val countingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten, total)
            }
        }
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}

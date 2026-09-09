package com.kmpbits.netflow_core.builders

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

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

    return builder.build()
}

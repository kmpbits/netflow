package com.kmpbits.netflow_core.builders

/** One collected part of a `multipart/form-data` body. */
sealed interface MultipartPart {
    val name: String

    /** A text form field. [contentType] is null unless set explicitly. */
    data class Field(
        override val name: String,
        val value: String,
        val contentType: String?,
    ) : MultipartPart

    /** A binary file field. */
    class File(
        override val name: String,
        val filename: String,
        val bytes: ByteArray,
        val contentType: String,
    ) : MultipartPart {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is File) return false
            return name == other.name &&
                filename == other.filename &&
                contentType == other.contentType &&
                bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + filename.hashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }

        override fun toString(): String =
            "File(name=$name, filename=$filename, contentType=$contentType, bytes=${bytes.size} bytes)"
    }
}

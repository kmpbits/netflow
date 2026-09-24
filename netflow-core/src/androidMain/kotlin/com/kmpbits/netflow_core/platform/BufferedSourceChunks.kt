package com.kmpbits.netflow_core.platform

import okio.BufferedSource

/**
 * Reads the source to exhaustion, handing each read (at most [chunkSize] bytes) to [emit].
 * [onProgress] gets the cumulative bytes read and [total] (`-1` if unknown) after each read,
 * before the chunk is handed to [emit].
 */
internal suspend fun BufferedSource.readChunks(
    chunkSize: Int = 8192,
    total: Long = -1L,
    onProgress: ((received: Long, total: Long) -> Unit)? = null,
    emit: suspend (ByteArray) -> Unit,
) {
    val buffer = ByteArray(chunkSize)
    var received = 0L
    while (true) {
        val read = read(buffer)
        if (read == -1) return
        if (read > 0) {
            received += read
            onProgress?.invoke(received, total)
            emit(buffer.copyOf(read))
        }
    }
}

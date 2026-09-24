package com.kmpbits.netflow_core.platform

import okio.BufferedSource

/** Reads the source to exhaustion, handing each read (at most [chunkSize] bytes) to [emit]. */
internal suspend fun BufferedSource.readChunks(
    chunkSize: Int = 8192,
    emit: suspend (ByteArray) -> Unit,
) {
    val buffer = ByteArray(chunkSize)
    while (true) {
        val read = read(buffer)
        if (read == -1) return
        if (read > 0) emit(buffer.copyOf(read))
    }
}

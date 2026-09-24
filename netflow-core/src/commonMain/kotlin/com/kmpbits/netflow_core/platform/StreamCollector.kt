package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.exceptions.HttpException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Bridges push-style callbacks (the iOS `NSURLSessionDataDelegate`) into a [Flow] of
 * response chunks. It has no platform dependency so it is unit-testable on any target.
 *
 * Callbacks ([onResponse], [onData], [onComplete]) come serially from one delegate queue; the
 * [flow] is collected from a coroutine. Backpressure: the channel is unbounded (a callback must
 * never block), so once [capacity] chunks are waiting [onSuspend] is invoked once — the caller
 * pauses the network task — and [onResume] once the consumer has drained to half of [capacity].
 *
 * [onProgress] gets the cumulative bytes received (and the content length, `-1` if unknown) as each
 * chunk arrives from the network, before it is handed to the collector. It is not called for a
 * non-2xx body.
 *
 * A non-2xx status is turned into an [HttpException] carrying the accumulated body; its chunks are
 * never emitted. A completion error is delivered after the chunks already received.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class StreamCollector(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val onSuspend: () -> Unit,
    private val onResume: () -> Unit,
    private val onProgress: ((received: Long, total: Long) -> Unit)? = null,
) {
    private val channel = Channel<ByteArray>(Channel.UNLIMITED)
    private val pending = AtomicInt(0)
    private val paused = AtomicBoolean(false)

    private var failedCode: Int? = null
    private var contentLength = -1L
    private var received = 0L
    private val errorChunks = mutableListOf<ByteArray>()

    /** [contentLength] is the expected body size, or `-1` when unknown. */
    fun onResponse(code: Int, contentLength: Long = -1L) {
        if (code !in 200..299) failedCode = code
        this.contentLength = contentLength
    }

    fun onData(bytes: ByteArray) {
        if (failedCode != null) {
            errorChunks += bytes
            return
        }
        received += bytes.size
        onProgress?.invoke(received, contentLength)
        channel.trySend(bytes)
        if (pending.addAndFetch(1) >= capacity && paused.compareAndSet(false, true)) {
            onSuspend()
        }
    }

    fun onComplete(error: Throwable?) {
        val code = failedCode
        when {
            code != null -> channel.close(HttpException(code, errorChunks.joinToBytes().decodeToString()))
            error != null -> channel.close(error)
            else -> channel.close()
        }
    }

    val flow: Flow<ByteArray> = flow {
        try {
            for (chunk in channel) {
                val left = pending.addAndFetch(-1)
                if (left <= capacity / 2 && paused.compareAndSet(true, false)) onResume()
                emit(chunk)
            }
        } finally {
            channel.cancel()
        }
    }

    private fun List<ByteArray>.joinToBytes(): ByteArray {
        val out = ByteArray(sumOf { it.size })
        var offset = 0
        for (part in this) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }

    companion object {
        const val DEFAULT_CAPACITY = 64
    }
}

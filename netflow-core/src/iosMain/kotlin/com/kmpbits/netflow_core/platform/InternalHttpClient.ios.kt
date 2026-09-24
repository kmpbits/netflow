package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.alias.Header
import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.extensions.toByteArray
import com.kmpbits.netflow_core.enums.HttpHeader
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.pinning.NetFlowSessionDelegate
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDataTask
import platform.Foundation.dataTaskWithRequest
import kotlin.coroutines.resumeWithException

internal actual class InternalHttpClient(
    private val session: NSURLSession,
    private val delegate: NetFlowSessionDelegate,
) {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse {
        return suspendCancellableCoroutine { continuation ->
            lateinit var task: NSURLSessionDataTask

            task = session.dataTaskWithRequest(
                request = requestBuilder.request,
                completionHandler = { data, response, error ->
                    delegate.clearProgress(task)
                    when {
                        error != null -> {
                            continuation.resumeWithException(HttpException(error.code.convert(), error.localizedDescription))
                        }
                        data != null && response is NSHTTPURLResponse -> {
                            val bodyByteArray = data.toByteArray()
                            val statusCode = response.statusCode.toInt()

                            continuation.resumeWith(
                                Result.success(
                                    NetFlowResponse(
                                        code = statusCode,
                                        headers = response.allHeaderFields.map { (name, value) ->
                                            Header(HttpHeader.custom(name.toString()), value.toString())
                                        },
                                        body = bodyByteArray.decodeToString(),
                                        errorBody = null
                                    )
                                )
                            )
                        }
                        else -> {
                            continuation.resumeWithException(Throwable("Something went wrong"))
                        }
                    }
                }
            )

            builder.onProgress?.let { delegate.registerProgress(task, it) }

            continuation.invokeOnCancellation {
                delegate.clearProgress(task)
                task.cancel()
            }

            task.resume()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun stream(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): Flow<ByteArray> = flow {
        lateinit var task: NSURLSessionDataTask
        val collector = StreamCollector(
            onSuspend = { task.suspend() },
            onResume = { task.resume() }, // suspend/resume are counted by NSURLSession; this pairs with onSuspend
            onProgress = builder.downloadProgress,
        )
        task = session.dataTaskWithRequest(requestBuilder.request)
        delegate.registerStream(task, collector)
        try {
            task.resume()
            emitAll(collector.flow)
        } finally {
            delegate.clearStream(task)
            task.cancel()
        }
    }
}

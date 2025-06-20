package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.extensions.toByteArray
import com.kmpbits.netflow_core.exceptions.HttpException
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import kotlin.coroutines.resumeWithException

internal actual class InternalHttpClient(
    private val session: NSURLSession
) {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse {
        return suspendCancellableCoroutine { continuation ->
            val task = session.dataTaskWithRequest(
                request = requestBuilder.request,
                completionHandler = { data, response, error ->
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
                                        headers = builder.headers,
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

            continuation.invokeOnCancellation {
                task.cancel()
            }

            task.resume()
        }
    }
}
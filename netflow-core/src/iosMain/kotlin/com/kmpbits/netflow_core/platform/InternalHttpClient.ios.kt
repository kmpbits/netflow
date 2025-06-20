package com.kmpbits.netflow_core.platform

import com.kmpbits.netflow_core.builders.RequestBuilder
import com.kmpbits.netflow_core.builders.extensions.toByteArray
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest

internal actual class InternalHttpClient {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun call(requestBuilder: InternalHttpRequestBuilder, builder: RequestBuilder): NetFlowResponse {
        return suspendCancellableCoroutine { continuation ->
            val task = NSURLSession.sharedSession.dataTaskWithRequest(
                request = requestBuilder.request,
                completionHandler = { data, response, error ->
                    when {
                        error != null -> {
                            continuation.resumeWith(Result.success(
                                NetFlowResponse(
                                    code = error.code.convert(),
                                    headers = builder.headers,
                                    body = null,
                                    errorBody = error.localizedDescription
                                )
                            ))
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
                            continuation.resumeWith(Result.success(
                                NetFlowResponse(
                                    code = 500,
                                    headers = builder.headers,
                                    body = null,
                                    errorBody = "Generic Error"
                                )
                            ))
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
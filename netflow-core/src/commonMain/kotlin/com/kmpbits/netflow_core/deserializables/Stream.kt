package com.kmpbits.netflow_core.deserializables

import com.kmpbits.netflow_core.request.NetFlowCall
import com.kmpbits.netflow_core.request.NetFlowRequest
import kotlinx.coroutines.flow.Flow

/**
 * The raw response body as it arrives, in chunks, without buffering it in memory.
 * Non-2xx statuses and transport failures make the flow throw (`HttpException` for the former);
 * see [NetFlowRequest.stream] for the auth/retry rules.
 */
fun NetFlowRequest.responseStream(): Flow<ByteArray> = stream()

fun NetFlowCall.responseStream(): Flow<ByteArray> = request.stream()

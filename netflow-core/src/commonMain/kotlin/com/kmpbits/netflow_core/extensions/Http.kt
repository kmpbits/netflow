package com.kmpbits.netflow_core.extensions

import com.kmpbits.netflow_core.builders.ClientBuilder
import com.kmpbits.netflow_core.client.NetFlowClient

/**
 * Initiate and retrieve the [NetFlowClient] instance with the [ClientBuilder]
 *
 * @return The client to start a new request
 */
fun netflowClient(builder: ClientBuilder. () -> Unit): NetFlowClient {
    return ClientBuilder().also(builder).build()
}
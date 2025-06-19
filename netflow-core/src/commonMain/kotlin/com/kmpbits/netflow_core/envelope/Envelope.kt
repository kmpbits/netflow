package com.kmpbits.netflow_core.envelope

import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T> internal constructor(
    val data: T?
)
package com.kmpbits.netflow_core.envelope

import kotlinx.serialization.Serializable

@Serializable
data class EnvelopeList<T> internal constructor(
    val data: List<T>
)

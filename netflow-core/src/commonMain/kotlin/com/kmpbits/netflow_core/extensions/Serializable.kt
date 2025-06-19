package com.kmpbits.netflow_core.extensions

import com.kmpbits.netflow_core.envelope.Envelope
import com.kmpbits.netflow_core.envelope.EnvelopeList
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

@PublishedApi
internal fun json(): Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    allowStructuredMapKeys = true
    prettyPrint = false
}

internal inline fun <reified T> T.toJson(): String {
    return json().encodeToString(this)
}

@PublishedApi
internal inline fun <reified T> NetFlowResponse.toList(): List<T> {
    val envelopeListSerializer: KSerializer<EnvelopeList<T>> = EnvelopeList.serializer(serializer())
    return json().decodeFromString(envelopeListSerializer, body.orEmpty()).data
}

@PublishedApi
internal inline fun <reified T> NetFlowResponse.toModelWrapped(): T? {
    val envelopeSerializer: KSerializer<Envelope<T>> = Envelope.serializer(serializer())
    return json().decodeFromString(envelopeSerializer, body.orEmpty()).data
}

@PublishedApi
internal inline fun <reified T> NetFlowResponse.toModel(): T {
    return json().decodeFromString(serializer(), body.orEmpty())
}

@PublishedApi
internal inline fun <reified T> NetFlowResponse.toEnvelopeList(): EnvelopeList<T> {
    val envelopeListSerializer: KSerializer<EnvelopeList<T>> = EnvelopeList.serializer(serializer())
    return json().decodeFromString(envelopeListSerializer, body.orEmpty())
}

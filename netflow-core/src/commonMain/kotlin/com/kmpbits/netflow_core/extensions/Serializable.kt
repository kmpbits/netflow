package com.kmpbits.netflow_core.extensions

import com.kmpbits.netflow_core.envelope.Envelope
import com.kmpbits.netflow_core.envelope.EnvelopeList
import com.kmpbits.netflow_core.response.NetFlowResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

fun Map<String, Any?>.toJson(): String {
    val jsonMap = this.mapValues { (_, value) ->
        when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            null -> JsonNull
            else -> throw IllegalArgumentException("Unsupported value type: ${value::class.simpleName}")
        }
    }
    return JsonObject(jsonMap).toString()
}

@PublishedApi
internal inline fun <reified T> NetFlowResponse.toList(): List<T> {
    val listSerializer: KSerializer<List<T>> = ListSerializer(serializer())
    return json().decodeFromString(listSerializer, body.orEmpty())
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

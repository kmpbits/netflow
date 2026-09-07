package com.kmpbits.netflow_core.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Reads unverified claims from a JWT access token. The server verifies signatures; we only peek. */
@OptIn(ExperimentalEncodingApi::class)
internal object Jwt {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalEncodingApi::class)
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    /**
     * The `exp` claim as epoch seconds, or null when [token] is not a JWT, has no
     * `exp`, or cannot be parsed (e.g. an opaque token).
     */
    fun expiresAtEpochSeconds(token: String): Long? = runCatching {
        val payload = token.split('.').getOrNull(1) ?: return null
        val decoded = base64.decode(payload).decodeToString()
        json.parseToJsonElement(decoded).jsonObject["exp"]?.jsonPrimitive?.longOrNull
    }.getOrNull()
}

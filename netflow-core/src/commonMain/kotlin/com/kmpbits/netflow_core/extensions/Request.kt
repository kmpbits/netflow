package com.kmpbits.netflow_core.extensions

import com.kmpbits.netflow_core.alias.Parameters
import com.kmpbits.netflow_core.enums.HttpMethod

internal fun encodeParameterInUrl(method: HttpMethod): Boolean = when(method) {
    HttpMethod.Get, HttpMethod.Delete, HttpMethod.Head -> true
    else -> false
}

internal fun queryFromParameters(parameters: Parameters?): String = parameters.orEmpty()
    .filterNot { it.second == null }
    .joinToString("&") { (key, value) -> "$key=$value" }

internal fun createUrl(
    baseUrl: String,
    path: String
): String {
    return baseUrl + if (path.startsWith('/') or path.isEmpty()) path else "/$path"
}

internal fun urlWithPath(
    baseUrl: String,
    path: String,
    method: HttpMethod,
    parameters: Parameters?
): String {
    var modifiedPath = path

    if (encodeParameterInUrl(method)) {
        var querySign = ""
        val queryParameterString = queryFromParameters(parameters)

        if (queryParameterString.isNotEmpty()) {
            if (path.isNotEmpty())
                querySign = if (path.last() == '?') "" else "?"
        }

        modifiedPath += (querySign + queryParameterString)
    }

    return createUrl(baseUrl, modifiedPath)
}
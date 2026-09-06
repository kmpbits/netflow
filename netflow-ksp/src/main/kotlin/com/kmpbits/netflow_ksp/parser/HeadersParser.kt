package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ParseContext

/** Parses `@Headers("Name: Value", ...)` on [fn] into (name, value) pairs. */
internal fun parseStaticHeaders(
    fn: KSFunctionDeclaration,
    ctx: ParseContext,
): List<Pair<String, String>> {
    val name = fn.simpleName.asString()
    val annotation = fn.annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == Fqns.HEADERS
    } ?: return emptyList()

    @Suppress("UNCHECKED_CAST")
    val entries = (annotation.arguments.firstOrNull { it.name?.asString() == "value" }?.value as? List<String>)
        ?: emptyList()

    return entries.mapNotNull { entry ->
        val colon = entry.indexOf(':')
        val headerName = if (colon >= 0) entry.substring(0, colon).trim() else ""
        val headerValue = if (colon >= 0) entry.substring(colon + 1).trim() else ""
        if (colon < 0 || headerName.isEmpty()) {
            ctx.error("@Headers entry \"$entry\" on '$name' is not in \"Name: Value\" form.", fn)
            null
        } else {
            headerName to headerValue
        }
    }
}

package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.HttpMethodName
import com.kmpbits.netflow_ksp.model.ParseContext

internal fun parseHttpMethod(
    fn: KSFunctionDeclaration,
    ctx: ParseContext,
): Pair<HttpMethodName, String>? {
    val methodAnnotations = fn.annotations.filter {
        it.annotationType.resolve().declaration.qualifiedName?.asString() in Fqns.HTTP_METHOD_ANNOTATIONS
    }.toList()

    val name = fn.simpleName.asString()
    if (methodAnnotations.isEmpty()) {
        ctx.error(
            "Function '$name' has no HTTP method annotation (@GET/@POST/@PUT/@DELETE/@PATCH).",
            fn,
        )
        return null
    }
    if (methodAnnotations.size > 1) {
        ctx.error("Function '$name' must have exactly one HTTP method annotation.", fn)
        return null
    }

    val annotation = methodAnnotations.single()
    val fqn = annotation.annotationType.resolve().declaration.qualifiedName!!.asString()
    val method = when (fqn) {
        Fqns.GET -> HttpMethodName.GET
        Fqns.POST -> HttpMethodName.POST
        Fqns.PUT -> HttpMethodName.PUT
        Fqns.DELETE -> HttpMethodName.DELETE
        Fqns.PATCH -> HttpMethodName.PATCH
        else -> {
            ctx.error("Unknown HTTP method annotation on '$name'.", fn)
            return null
        }
    }

    val path = annotation.arguments
        .firstOrNull { it.name?.asString() == "path" }?.value as? String
        ?: (annotation.arguments.firstOrNull()?.value as? String)
        ?: ""

    return method to path
}

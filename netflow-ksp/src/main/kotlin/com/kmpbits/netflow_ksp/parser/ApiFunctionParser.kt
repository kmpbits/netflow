package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ApiFunction
import com.kmpbits.netflow_ksp.model.ParamBinding
import com.kmpbits.netflow_ksp.model.ParseContext

private val PLACEHOLDER = Regex("\\{([A-Za-z_][A-Za-z0-9_]*)}")

internal fun parseApiFunction(
    declaration: KSFunctionDeclaration,
    resolver: Resolver,
    ctx: ParseContext,
): ApiFunction? {
    val name = declaration.simpleName.asString()
    val isSuspend = Modifier.SUSPEND in declaration.modifiers

    val method = parseHttpMethod(declaration, ctx) ?: return null
    val (httpMethod, pathTemplate) = method

    val returnShape = parseReturnShape(declaration, isSuspend, ctx) ?: return null

    val staticHeaders = parseStaticHeaders(declaration, ctx)
    val wrapped = declaration.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == Fqns.WRAPPED
    }

    val parameters = declaration.parameters.mapNotNull { parseParameter(it, ctx) }

    if (parameters.filterIsInstance<ParamBinding.BodyParam>().size > 1) {
        ctx.error("Function '$name' has more than one @Body; at most one @Body is allowed.", declaration)
    }

    val placeholders = PLACEHOLDER.findAll(pathTemplate).map { it.groupValues[1] }.toSet()
    val pathParams = parameters.filterIsInstance<ParamBinding.PathParam>()
    val pathWireNames = pathParams.map { it.wireName }.toSet()

    (placeholders - pathWireNames).forEach {
        ctx.error("Function '$name' path declares '{$it}' but there is no @Path parameter named '$it'.", declaration)
    }
    (pathWireNames - placeholders).forEach { wire ->
        ctx.error("Function '$name' @Path '$wire' has no matching '{$wire}' placeholder in the path.", declaration)
    }

    if (ctx.hasErrors) return null

    return ApiFunction(
        declaration = declaration,
        simpleName = name,
        httpMethod = httpMethod,
        pathTemplate = pathTemplate,
        isSuspend = isSuspend,
        returnShape = returnShape,
        parameters = parameters,
        wrapped = wrapped,
        staticHeaders = staticHeaders,
    )
}

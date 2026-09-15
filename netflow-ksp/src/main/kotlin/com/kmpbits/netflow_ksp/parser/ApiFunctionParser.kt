package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ApiFunction
import com.kmpbits.netflow_ksp.model.ParamBinding
import com.kmpbits.netflow_ksp.model.ParseContext
import com.kmpbits.netflow_ksp.model.ReturnShape

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
    val skipAuth = declaration.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == Fqns.SKIP_AUTH
    }
    val multipart = declaration.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == Fqns.MULTIPART
    }

    val paging = parsePagingConfig(
        declaration,
        isPaginated = returnShape is ReturnShape.Paginated,
        ctx,
    )

    val parameters = declaration.parameters.mapNotNull { parseParameter(it, ctx) }

    if (parameters.filterIsInstance<ParamBinding.BodyParam>().size > 1) {
        ctx.error("Function '$name' has more than one @Body; at most one @Body is allowed.", declaration)
    }

    val partParams = parameters.filterIsInstance<ParamBinding.PartParam>()
    val hasBody = parameters.any { it is ParamBinding.BodyParam }
    if (partParams.isNotEmpty() && !multipart) {
        ctx.error(
            "@Part parameter '${partParams.first().paramName}' requires @Multipart on function '$name'.",
            declaration,
        )
    }
    if (multipart && hasBody) {
        ctx.error("@Multipart function '$name' cannot also use @Body.", declaration)
    }
    if (multipart && partParams.isEmpty()) {
        ctx.error("@Multipart function '$name' has no @Part parameters.", declaration)
    }

    val urlParams = parameters.filterIsInstance<ParamBinding.UrlParam>()
    val pathParamsForUrlCheck = parameters.filterIsInstance<ParamBinding.PathParam>()
    if (urlParams.size > 1) {
        ctx.error("Function '$name' has more than one @Url; at most one is allowed.", declaration)
    }
    if (urlParams.isNotEmpty() && pathParamsForUrlCheck.isNotEmpty()) {
        ctx.error("Function '$name' cannot combine @Url with @Path.", declaration)
    }
    if (urlParams.isNotEmpty() && pathTemplate.isNotEmpty()) {
        ctx.error("Function '$name' has @Url — its @GET/@POST/... path must be empty.", declaration)
    }
    if (urlParams.isEmpty() && pathTemplate.isEmpty()) {
        ctx.error("Function '$name' has an empty path and no @Url parameter.", declaration)
    }

    val queryMapParams = parameters.filterIsInstance<ParamBinding.QueryMapParam>()
    if (queryMapParams.size > 1) {
        ctx.error("Function '$name' has more than one @QueryMap; at most one is allowed.", declaration)
    }
    val headerMapParams = parameters.filterIsInstance<ParamBinding.HeaderMapParam>()
    if (headerMapParams.size > 1) {
        ctx.error("Function '$name' has more than one @HeaderMap; at most one is allowed.", declaration)
    }

    if (returnShape is ReturnShape.RawCall && wrapped) {
        ctx.error(
            "Function '$name' returns NetFlowCall — @Wrapped is the caller's response-strategy choice, not the interface's.",
            declaration,
        )
    }

    if (returnShape is ReturnShape.Model && wrapped) {
        ctx.error(
            "Function '$name' — @Wrapped is not supported with a bare model return. " +
                "Use AsyncState<T> with @Wrapped, or return NetFlowCall.",
            declaration,
        )
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
        skipAuth = skipAuth,
        multipart = multipart,
        staticHeaders = staticHeaders,
        paging = paging,
    )
}

package com.kmpbits.netflow_ksp.codegen

import com.google.devtools.ksp.symbol.KSType
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ApiFunction
import com.kmpbits.netflow_ksp.model.ParamBinding
import com.kmpbits.netflow_ksp.model.PagingConfig
import com.kmpbits.netflow_ksp.model.PartKind
import com.kmpbits.netflow_ksp.model.ReturnShape
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toTypeName

private val HTTP_METHOD = ClassName(Fqns.ENUMS_PKG, "HttpMethod")
private val HTTP_HEADER = ClassName(Fqns.ENUMS_PKG, "HttpHeader")
private val OPT_IN = ClassName("kotlin", "OptIn")
private val EXPERIMENTAL_PAGING_API = ClassName("androidx.paging", "ExperimentalPagingApi")
private val RESPONSE_PAGINATED = MemberName(Fqns.PAGING_DESERIALIZABLES_PKG, "responsePaginated")
private val PREPARE_CALL = MemberName("com.kmpbits.netflow_core.client", "prepareCall")
private val MULTIPART_MEMBER = MemberName("com.kmpbits.netflow_core.builders", "multipart")
private val PLACEHOLDER = Regex("\\{([A-Za-z_][A-Za-z0-9_]*)}")

internal fun buildFunction(fn: ApiFunction): FunSpec {
    val builder = FunSpec.builder(fn.simpleName).addModifiers(KModifier.OVERRIDE)
    if (fn.isSuspend) builder.addModifiers(KModifier.SUSPEND)

    if (fn.returnShape is ReturnShape.Paginated) {
        builder.addAnnotation(
            AnnotationSpec.builder(OPT_IN).addMember("%T::class", EXPERIMENTAL_PAGING_API).build()
        )
    }

    fn.parameters.forEach { param ->
        builder.addParameter(param.paramName, param.type.toTypeName())
    }
    builder.returns(fn.declaration.returnType!!.toTypeName())

    val code = CodeBlock.builder()
    if (fn.returnShape is ReturnShape.RawCall) {
        code.beginControlFlow("return client.%M", PREPARE_CALL)
    } else {
        code.beginControlFlow("return client.call")
    }
    code.addStatement("method = %T.%L", HTTP_METHOD, fn.httpMethod.enumMember)
    code.addStatement("path = %L", buildPathExpression(fn))
    if (fn.skipAuth) code.addStatement("skipAuth()")
    fn.staticHeaders.forEach { (headerName, headerValue) ->
        code.addStatement("header(%T.custom(%S) to %S)", HTTP_HEADER, headerName, headerValue)
    }
    if (fn.multipart) {
        code.add(buildMultipartBlock(fn))
    }
    fn.parameters.forEach { param ->
        buildParamStatement(param)?.let { code.addStatement("%L", it) }
    }
    code.endControlFlow()

    when (val shape = fn.returnShape) {
        is ReturnShape.Paginated -> {
            val payload = shape.payloadType.toTypeName()
            val cfg = fn.paging ?: PagingConfig.DEFAULT
            code.beginControlFlow(".%M<%T, %T>", RESPONSE_PAGINATED, payload, payload)
            code.addStatement("onlyApiCall = true")
            if (cfg.pageQueryName != "page") code.addStatement("pageQueryName = %S", cfg.pageQueryName)
            if (cfg.pageSize != 20) code.addStatement("defaultPageSize = %L", cfg.pageSize)
            if (fn.wrapped) code.addStatement("wrappedResponse = true")
            code.endControlFlow()
        }
        is ReturnShape.RawCall -> {
            // no response strategy — the chain ends at the prepareCall { } block
        }
        else -> {
            val (member, payload) = responseCall(shape, fn.wrapped)
            code.add(".%M<%T>()\n", member, payload.toTypeName())
        }
    }

    builder.addCode(code.build())
    return builder.build()
}

private fun responseCall(shape: ReturnShape, wrapped: Boolean): Pair<MemberName, KSType> {
    val name = when (shape) {
        is ReturnShape.FlowSingle -> if (wrapped) "responseWrappedFlow" else "responseFlow"
        is ReturnShape.FlowList -> if (wrapped) "responseWrappedListFlow" else "responseListFlow"
        is ReturnShape.AsyncSingle -> if (wrapped) "responseWrappedAsync" else "responseAsync"
        is ReturnShape.AsyncList -> if (wrapped) "responseWrappedListAsync" else "responseListAsync"
        is ReturnShape.Model -> "responseToModel"
        is ReturnShape.Paginated -> error("Paginated is handled separately in buildFunction")
        is ReturnShape.RawCall -> error("RawCall is handled separately in buildFunction")
    }
    return MemberName(Fqns.DESERIALIZABLES_PKG, name) to shape.payloadType!!
}

/** The right-hand side of `path = ...` — a string literal or a `"a/" + id + "/b"` expression. */
private fun buildPathExpression(fn: ApiFunction): CodeBlock {
    val template = fn.pathTemplate
    val matches = PLACEHOLDER.findAll(template).toList()
    if (matches.isEmpty()) return CodeBlock.of("%S", template)

    val wireToParam = fn.parameters.filterIsInstance<ParamBinding.PathParam>()
        .associate { it.wireName to it.paramName }

    val parts = mutableListOf<CodeBlock>()
    var index = 0
    for (match in matches) {
        val literal = template.substring(index, match.range.first)
        if (literal.isNotEmpty()) parts += CodeBlock.of("%S", literal)
        parts += CodeBlock.of("%N", wireToParam.getValue(match.groupValues[1]))
        index = match.range.last + 1
    }
    val tail = template.substring(index)
    if (tail.isNotEmpty()) parts += CodeBlock.of("%S", tail)

    return parts.joinToCodeWith(" + ")
}

/** A single statement inside the `client.call { }` lambda, or null when the binding is handled elsewhere. */
private fun buildParamStatement(param: ParamBinding): CodeBlock? = when (param) {
    is ParamBinding.PathParam -> null
    is ParamBinding.QueryParam ->
        if (param.isNullable) {
            CodeBlock.of("if (%N != null) parameter(%S to %N)", param.paramName, param.wireName, param.paramName)
        } else {
            CodeBlock.of("parameter(%S to %N)", param.wireName, param.paramName)
        }
    is ParamBinding.HeaderParam ->
        if (param.isNullable) {
            CodeBlock.of(
                "if (%N != null) header(%T.custom(%S) to %N.toString())",
                param.paramName, HTTP_HEADER, param.wireName, param.paramName,
            )
        } else {
            CodeBlock.of(
                "header(%T.custom(%S) to %N.toString())",
                HTTP_HEADER, param.wireName, param.paramName,
            )
        }
    is ParamBinding.BodyParam -> CodeBlock.of("body(%N)", param.paramName)
    is ParamBinding.PartParam -> null // emitted by buildMultipartBlock
    is ParamBinding.UrlParam -> null // consumed by buildPathExpression's `path = ...`
    is ParamBinding.QueryMapParam -> null // Task 4 replaces this with the forEach loop
    is ParamBinding.HeaderMapParam -> null // Task 4 replaces this with the forEach loop
}

/** The `multipart { … }` sub-block: one statement per `@Part`, nullable parts guarded. */
private fun buildMultipartBlock(fn: ApiFunction): CodeBlock {
    val block = CodeBlock.builder()
    block.beginControlFlow("%M", MULTIPART_MEMBER)
    fn.parameters.filterIsInstance<ParamBinding.PartParam>().forEach { part ->
        val statement = when (part.kind) {
            PartKind.FILE_PART -> CodeBlock.of("filePart(%S, %N)", part.wireName, part.paramName)
            PartKind.PRIMITIVE -> CodeBlock.of("part(%S, %N)", part.wireName, part.paramName)
            PartKind.SERIALIZABLE -> CodeBlock.of("jsonPart(%S, %N)", part.wireName, part.paramName)
        }
        if (part.isNullable) {
            block.beginControlFlow("if (%N != null)", part.paramName)
            block.addStatement("%L", statement)
            block.endControlFlow()
        } else {
            block.addStatement("%L", statement)
        }
    }
    block.endControlFlow()
    return block.build()
}

private fun List<CodeBlock>.joinToCodeWith(separator: String): CodeBlock =
    CodeBlock.builder().apply {
        this@joinToCodeWith.forEachIndexed { i, part ->
            if (i > 0) add(separator)
            add(part)
        }
    }.build()

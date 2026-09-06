package com.kmpbits.netflow_ksp.codegen

import com.google.devtools.ksp.symbol.KSType
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ApiFunction
import com.kmpbits.netflow_ksp.model.ParamBinding
import com.kmpbits.netflow_ksp.model.ReturnShape
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ksp.toTypeName

private val HTTP_METHOD = ClassName(Fqns.ENUMS_PKG, "HttpMethod")
private val HTTP_HEADER = ClassName(Fqns.ENUMS_PKG, "HttpHeader")
private val PLACEHOLDER = Regex("\\{([A-Za-z_][A-Za-z0-9_]*)}")

internal fun buildFunction(fn: ApiFunction): FunSpec {
    val builder = FunSpec.builder(fn.simpleName).addModifiers(KModifier.OVERRIDE)
    if (fn.isSuspend) builder.addModifiers(KModifier.SUSPEND)

    fn.parameters.forEach { param ->
        builder.addParameter(param.paramName, param.type.toTypeName())
    }
    builder.returns(fn.declaration.returnType!!.toTypeName())

    val code = CodeBlock.builder()
    code.beginControlFlow("return client.call")
    code.addStatement("method = %T.%L", HTTP_METHOD, fn.httpMethod.enumMember)
    code.addStatement("path = %L", buildPathExpression(fn))
    fn.staticHeaders.forEach { (headerName, headerValue) ->
        code.addStatement("header(%T.custom(%S) to %S)", HTTP_HEADER, headerName, headerValue)
    }
    fn.parameters.forEach { param ->
        buildParamStatement(param)?.let { code.addStatement("%L", it) }
    }
    code.endControlFlow()

    val (member, payload) = responseCall(fn.returnShape, fn.wrapped)
    code.add(".%M<%T>()\n", member, payload.toTypeName())

    builder.addCode(code.build())
    return builder.build()
}

private fun responseCall(shape: ReturnShape, wrapped: Boolean): Pair<MemberName, KSType> {
    val name = when (shape) {
        is ReturnShape.FlowSingle -> if (wrapped) "responseWrappedFlow" else "responseFlow"
        is ReturnShape.FlowList -> if (wrapped) "responseWrappedListFlow" else "responseListFlow"
        is ReturnShape.AsyncSingle -> if (wrapped) "responseWrappedAsync" else "responseAsync"
        is ReturnShape.AsyncList -> if (wrapped) "responseWrappedListAsync" else "responseListAsync"
    }
    return MemberName(Fqns.DESERIALIZABLES_PKG, name) to shape.payloadType
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
}

private fun List<CodeBlock>.joinToCodeWith(separator: String): CodeBlock =
    CodeBlock.builder().apply {
        this@joinToCodeWith.forEachIndexed { i, part ->
            if (i > 0) add(separator)
            add(part)
        }
    }.build()

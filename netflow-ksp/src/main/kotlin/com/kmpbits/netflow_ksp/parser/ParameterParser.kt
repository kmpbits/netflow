package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueParameter
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ParamBinding
import com.kmpbits.netflow_ksp.model.ParseContext
import com.kmpbits.netflow_ksp.model.PartKind

private val PART_PRIMITIVE_FQNS = setOf(
    "kotlin.String", "kotlin.Int", "kotlin.Long", "kotlin.Double", "kotlin.Boolean",
)

private fun isStringKeyedMap(type: KSType): Boolean =
    type.declaration.qualifiedName?.asString() == Fqns.MAP &&
        type.arguments.firstOrNull()?.type?.resolve()
            ?.declaration?.qualifiedName?.asString() == Fqns.STRING

private fun partKindOf(type: KSType): PartKind? {
    val declaration = type.declaration
    return when {
        declaration.qualifiedName?.asString() == Fqns.FILE_PART -> PartKind.FILE_PART
        declaration.qualifiedName?.asString() in PART_PRIMITIVE_FQNS -> PartKind.PRIMITIVE
        (declaration as? KSClassDeclaration)?.annotations?.any {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == Fqns.SERIALIZABLE
        } == true -> PartKind.SERIALIZABLE
        else -> null
    }
}

internal fun parseParameter(
    parameter: KSValueParameter,
    ctx: ParseContext,
): ParamBinding? {
    val paramName = parameter.name?.asString() ?: run {
        ctx.error("Encountered a parameter with no name.", parameter); return null
    }
    val type = parameter.type.resolve()
    val isNullable = type.isMarkedNullable

    val bindings = parameter.annotations.mapNotNull { annotation ->
        val fqn = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        val explicitName = annotation.arguments
            .firstOrNull { it.name?.asString() == "name" }?.value as? String
        val wireName = explicitName?.takeIf { it.isNotBlank() } ?: paramName
        when (fqn) {
            Fqns.PATH -> {
                if (isNullable) {
                    ctx.error("@Path parameter '$paramName' must not be nullable.", parameter)
                }
                ParamBinding.PathParam(paramName, wireName, type, isNullable = false)
            }
            Fqns.QUERY -> ParamBinding.QueryParam(paramName, wireName, type, isNullable)
            Fqns.HEADER -> ParamBinding.HeaderParam(paramName, wireName, type, isNullable)
            Fqns.BODY -> {
                ParamBinding.BodyParam(paramName, type, isNullable, isStringKeyedMap(type))
            }
            Fqns.PART -> {
                val kind = partKindOf(type)
                if (kind == null) {
                    ctx.error(
                        "@Part parameter '$paramName' must be FilePart, a primitive, or an @Serializable type.",
                        parameter,
                    )
                }
                // Return a binding even on error so parsing doesn't also report
                // "no binding annotation"; generation is gated on ctx.hasErrors.
                ParamBinding.PartParam(paramName, wireName, type, isNullable, kind ?: PartKind.PRIMITIVE)
            }
            Fqns.URL -> {
                if (isNullable || type.declaration.qualifiedName?.asString() != Fqns.STRING) {
                    ctx.error("@Url parameter '$paramName' must be a non-null String.", parameter)
                }
                ParamBinding.UrlParam(paramName, type)
            }
            Fqns.QUERY_MAP -> {
                if (!isStringKeyedMap(type)) {
                    ctx.error("@QueryMap parameter '$paramName' must be a Map<String, *>.", parameter)
                }
                ParamBinding.QueryMapParam(paramName, type, isNullable)
            }
            Fqns.HEADER_MAP -> {
                if (!isStringKeyedMap(type)) {
                    ctx.error("@HeaderMap parameter '$paramName' must be a Map<String, *>.", parameter)
                }
                ParamBinding.HeaderMapParam(paramName, type, isNullable)
            }
            else -> null
        }
    }.toList()

    when {
        bindings.isEmpty() -> {
            ctx.error(
                "Parameter '$paramName' has no NetFlow binding annotation (@Path/@Query/@Header/@Body/@Part).",
                parameter,
            )
            return null
        }
        bindings.size > 1 -> {
            ctx.error("Parameter '$paramName' has more than one NetFlow binding annotation.", parameter)
            return null
        }
    }
    return bindings.single()
}

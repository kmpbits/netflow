package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.symbol.KSValueParameter
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ParamBinding
import com.kmpbits.netflow_ksp.model.ParseContext

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
                val isStringKeyedMap = type.declaration.qualifiedName?.asString() == Fqns.MAP &&
                    type.arguments.firstOrNull()?.type?.resolve()
                        ?.declaration?.qualifiedName?.asString() == Fqns.STRING
                if (!isStringKeyedMap) {
                    ctx.error("@Body must be Map<String, *> in this version (parameter '$paramName').", parameter)
                }
                ParamBinding.BodyParam(paramName, type, isNullable)
            }
            else -> null
        }
    }.toList()

    when {
        bindings.isEmpty() -> {
            ctx.error(
                "Parameter '$paramName' has no NetFlow binding annotation (@Path/@Query/@Header/@Body).",
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

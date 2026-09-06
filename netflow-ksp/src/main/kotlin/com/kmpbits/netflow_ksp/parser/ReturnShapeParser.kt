package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ParseContext
import com.kmpbits.netflow_ksp.model.ReturnShape

internal fun parseReturnShape(
    fn: KSFunctionDeclaration,
    isSuspend: Boolean,
    ctx: ParseContext,
): ReturnShape? {
    val name = fn.simpleName.asString()
    val returnType = fn.returnType?.resolve()
    if (returnType == null) {
        ctx.error("Function '$name' has no resolvable return type.", fn)
        return null
    }

    fun fqnOf(type: KSType) = type.declaration.qualifiedName?.asString()
    fun firstArg(type: KSType): KSType? = type.arguments.firstOrNull()?.type?.resolve()
    fun isList(type: KSType) = fqnOf(type) == Fqns.LIST

    return when (fqnOf(returnType)) {
        Fqns.FLOW -> {
            if (isSuspend) {
                ctx.error("Function '$name' returns Flow and must not be suspend.", fn)
                return null
            }
            val resultState = firstArg(returnType)
            if (resultState == null || fqnOf(resultState) != Fqns.RESULT_STATE) {
                ctx.error("Function '$name' must return Flow<ResultState<T>>.", fn)
                return null
            }
            val payload = firstArg(resultState) ?: run {
                ctx.error("Function '$name' has an unresolvable ResultState payload.", fn); return null
            }
            if (isList(payload)) {
                val item = firstArg(payload) ?: run {
                    ctx.error("Function '$name' has an unresolvable List item type.", fn); return null
                }
                ReturnShape.FlowList(item)
            } else {
                ReturnShape.FlowSingle(payload)
            }
        }

        Fqns.ASYNC_STATE -> {
            if (!isSuspend) {
                ctx.error("Function '$name' returns AsyncState and must be suspend.", fn)
                return null
            }
            val payload = firstArg(returnType) ?: run {
                ctx.error("Function '$name' has an unresolvable AsyncState payload.", fn); return null
            }
            if (isList(payload)) {
                val item = firstArg(payload) ?: run {
                    ctx.error("Function '$name' has an unresolvable List item type.", fn); return null
                }
                ReturnShape.AsyncList(item)
            } else {
                ReturnShape.AsyncSingle(payload)
            }
        }

        else -> {
            ctx.error(
                "Function '$name' has an unsupported return type. " +
                    "Use Flow<ResultState<T>> (non-suspend) or AsyncState<T> (suspend).",
                fn,
            )
            null
        }
    }
}

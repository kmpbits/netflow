package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
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
        Fqns.NET_FLOW_CALL -> {
            // `suspend` is allowed but redundant: prepareCall { } builds synchronously,
            // and the responseX() the caller composes on the NetFlowCall is already suspend.
            ReturnShape.RawCall
        }

        Fqns.FLOW -> {
            if (isSuspend) {
                ctx.error("Function '$name' returns Flow and must not be suspend.", fn)
                return null
            }
            val inner = firstArg(returnType)
            if (inner == null) {
                ctx.error("Function '$name' must return Flow<ResultState<T>> or Flow<PagingData<T>>.", fn)
                return null
            }
            when (fqnOf(inner)) {
                Fqns.RESULT_STATE -> {
                    val payload = firstArg(inner) ?: run {
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
                Fqns.PAGING_DATA -> {
                    val payload = firstArg(inner) ?: run {
                        ctx.error("Function '$name' has an unresolvable PagingData payload.", fn); return null
                    }
                    val extendsPagingModel = (payload.declaration as? KSClassDeclaration)
                        ?.getAllSuperTypes()
                        ?.any { it.declaration.qualifiedName?.asString() == Fqns.PAGING_MODEL } == true
                    if (!extendsPagingModel) {
                        ctx.error("Function '$name' — Flow<PagingData<T>> requires T to extend PagingModel.", fn)
                        return null
                    }
                    ReturnShape.Paginated(payload)
                }
                else -> {
                    ctx.error("Function '$name' must return Flow<ResultState<T>> or Flow<PagingData<T>>.", fn)
                    return null
                }
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

        Fqns.UNIT -> {
            ctx.error(
                "Function '$name' returns Unit. Use AsyncState<Unit> (suspend) for a state result, " +
                    "or NetFlowCall to compose the response yourself.",
                fn,
            )
            null
        }

        else -> {
            if (!isSuspend) {
                ctx.error(
                    "Function '$name' has an unsupported return type. Use Flow<ResultState<T>> / " +
                        "Flow<PagingData<T>> (non-suspend), or AsyncState<T> / a bare model type (suspend).",
                    fn,
                )
                return null
            }
            // suspend + a plain type (model or List<model>) -> responseToModel<T>(), Retrofit-style:
            // returns the deserialized value or throws HttpException.
            ReturnShape.Model(returnType)
        }
    }
}

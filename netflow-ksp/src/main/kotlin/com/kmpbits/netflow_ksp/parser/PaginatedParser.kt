package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.PagingConfig
import com.kmpbits.netflow_ksp.model.ParseContext

internal fun parsePagingConfig(
    fn: KSFunctionDeclaration,
    isPaginated: Boolean,
    ctx: ParseContext,
): PagingConfig? {
    val annotation = fn.annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == Fqns.PAGINATED
    }

    if (annotation == null) return if (isPaginated) PagingConfig.DEFAULT else null

    if (!isPaginated) {
        ctx.error(
            "@Paginated on '${fn.simpleName.asString()}' requires a Flow<PagingData<T>> return type.",
            fn,
        )
        return null
    }

    val pageQueryName = annotation.arguments
        .firstOrNull { it.name?.asString() == "pageQueryName" }?.value as? String ?: "page"
    val pageSize = (annotation.arguments
        .firstOrNull { it.name?.asString() == "pageSize" }?.value as? Int) ?: 20

    return PagingConfig(pageQueryName, pageSize)
}

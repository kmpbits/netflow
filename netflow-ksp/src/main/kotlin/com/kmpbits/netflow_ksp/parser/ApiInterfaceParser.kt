package com.kmpbits.netflow_ksp.parser

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.kmpbits.netflow_ksp.model.ApiInterface
import com.kmpbits.netflow_ksp.model.ParseContext

internal fun parseApiInterface(
    declaration: KSClassDeclaration,
    resolver: Resolver,
    ctx: ParseContext,
): ApiInterface? {
    if (declaration.classKind != ClassKind.INTERFACE) {
        ctx.error("@NetFlowApi can only be applied to an interface.", declaration)
        return null
    }
    if (declaration.typeParameters.isNotEmpty()) {
        ctx.error("@NetFlowApi interfaces must not have type parameters.", declaration)
        return null
    }

    val functions = declaration.getDeclaredFunctions()
        .filter { it.isAbstract }
        .mapNotNull { fn -> parseApiFunction(fn, resolver, ctx) }
        .toList()

    return ApiInterface(
        declaration = declaration,
        packageName = declaration.packageName.asString(),
        simpleName = declaration.simpleName.asString(),
        functions = functions,
    )
}

package com.kmpbits.netflow_ksp.codegen

import com.kmpbits.netflow_ksp.Fqns
import com.kmpbits.netflow_ksp.model.ApiInterface
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

private val NET_FLOW_CLIENT = ClassName(Fqns.CLIENT_PKG, "NetFlowClient")

internal fun generateApiFile(api: ApiInterface): FileSpec {
    val implName = "_${api.simpleName}Impl"
    val ifaceType = ClassName(api.packageName, api.simpleName)

    val implType = TypeSpec.classBuilder(implName)
        .addModifiers(KModifier.INTERNAL)
        .addSuperinterface(ifaceType)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("client", NET_FLOW_CLIENT)
                .build()
        )
        .addProperty(
            PropertySpec.builder("client", NET_FLOW_CLIENT)
                .addModifiers(KModifier.PRIVATE)
                .initializer("client")
                .build()
        )
        .addFunctions(api.functions.map { buildFunction(it) })
        .build()

    val factory = FunSpec.builder("create${api.simpleName}")
        .receiver(NET_FLOW_CLIENT)
        .returns(ifaceType)
        .addStatement("return %L(this)", implName)
        .build()

    return FileSpec.builder(api.packageName, implName)
        .addType(implType)
        .addFunction(factory)
        .build()
}

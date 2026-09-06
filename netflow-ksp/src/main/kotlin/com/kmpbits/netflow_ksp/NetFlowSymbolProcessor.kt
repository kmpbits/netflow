package com.kmpbits.netflow_ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.kmpbits.netflow_ksp.codegen.generateApiFile
import com.kmpbits.netflow_ksp.model.ParseContext
import com.kmpbits.netflow_ksp.parser.parseApiInterface

class NetFlowSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Fqns.NET_FLOW_API).toList()
        val deferred = symbols.filterNot { it.validate() }

        symbols
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()
            .forEach { declaration ->
                val ctx = ParseContext()
                val api = parseApiInterface(declaration, resolver, ctx)

                ctx.diagnostics.forEach { logger.error("NetFlow: ${it.message}", it.node) }

                if (api != null && !ctx.hasErrors) {
                    val fileSpec = generateApiFile(api)
                    codeGenerator.createNewFile(
                        dependencies = Dependencies(aggregating = false, declaration.containingFile!!),
                        packageName = fileSpec.packageName,
                        fileName = fileSpec.name,
                    ).bufferedWriter().use { writer ->
                        fileSpec.writeTo(writer)
                    }
                }
            }

        return deferred
    }
}

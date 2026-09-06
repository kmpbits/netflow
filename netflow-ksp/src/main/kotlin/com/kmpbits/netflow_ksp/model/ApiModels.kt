package com.kmpbits.netflow_ksp.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType

internal data class Diagnostic(val message: String, val node: KSNode?)

internal class ParseContext {
    private val _diagnostics = mutableListOf<Diagnostic>()
    val diagnostics: List<Diagnostic> get() = _diagnostics

    fun error(message: String, node: KSNode?) {
        _diagnostics.add(Diagnostic(message, node))
    }

    val hasErrors: Boolean get() = _diagnostics.isNotEmpty()
}

internal enum class HttpMethodName(val enumMember: String) {
    GET("Get"), POST("Post"), PUT("Put"), DELETE("Delete"), PATCH("Patch")
}

internal sealed interface ReturnShape {
    val payloadType: KSType

    data class FlowSingle(override val payloadType: KSType) : ReturnShape
    data class FlowList(override val payloadType: KSType) : ReturnShape
    data class AsyncSingle(override val payloadType: KSType) : ReturnShape
    data class AsyncList(override val payloadType: KSType) : ReturnShape
}

internal sealed interface ParamBinding {
    val paramName: String
    val wireName: String
    val type: KSType
    val isNullable: Boolean

    data class PathParam(
        override val paramName: String,
        override val wireName: String,
        override val type: KSType,
        override val isNullable: Boolean,
    ) : ParamBinding

    data class QueryParam(
        override val paramName: String,
        override val wireName: String,
        override val type: KSType,
        override val isNullable: Boolean,
    ) : ParamBinding

    data class HeaderParam(
        override val paramName: String,
        override val wireName: String,
        override val type: KSType,
        override val isNullable: Boolean,
    ) : ParamBinding

    data class BodyParam(
        override val paramName: String,
        override val type: KSType,
        override val isNullable: Boolean,
    ) : ParamBinding {
        override val wireName: String get() = paramName
    }
}

internal data class ApiFunction(
    val declaration: KSFunctionDeclaration,
    val simpleName: String,
    val httpMethod: HttpMethodName,
    val pathTemplate: String,
    val isSuspend: Boolean,
    val returnShape: ReturnShape,
    val parameters: List<ParamBinding>,
)

internal data class ApiInterface(
    val declaration: KSClassDeclaration,
    val packageName: String,
    val simpleName: String,
    val functions: List<ApiFunction>,
)

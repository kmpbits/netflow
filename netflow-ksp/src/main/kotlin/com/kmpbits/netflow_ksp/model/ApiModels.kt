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

internal enum class PartKind { FILE_PART, PRIMITIVE, SERIALIZABLE }

internal data class PagingConfig(val pageQueryName: String, val pageSize: Int) {
    companion object {
        val DEFAULT = PagingConfig(pageQueryName = "page", pageSize = 20)
    }
}

internal sealed interface ReturnShape {
    val payloadType: KSType?

    data class FlowSingle(override val payloadType: KSType) : ReturnShape
    data class FlowList(override val payloadType: KSType) : ReturnShape
    data class AsyncSingle(override val payloadType: KSType) : ReturnShape
    data class AsyncList(override val payloadType: KSType) : ReturnShape

    /** A bare model / `List<model>` return on a suspend function -> `responseToModel<T>()`. */
    data class Model(override val payloadType: KSType) : ReturnShape

    data class Paginated(override val payloadType: KSType) : ReturnShape
    data object RawCall : ReturnShape {
        override val payloadType: KSType? get() = null
    }
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
        val isStringKeyedMap: Boolean,
    ) : ParamBinding {
        override val wireName: String get() = paramName
    }

    data class PartParam(
        override val paramName: String,
        override val wireName: String,
        override val type: KSType,
        override val isNullable: Boolean,
        val kind: PartKind,
    ) : ParamBinding

    data class ProgressParam(
        override val paramName: String,
        override val type: KSType,
        override val isNullable: Boolean,
    ) : ParamBinding {
        override val wireName: String get() = paramName
    }

    data class UrlParam(
        override val paramName: String,
        override val type: KSType,
    ) : ParamBinding {
        override val wireName: String get() = paramName
        override val isNullable: Boolean get() = false
    }

    data class QueryMapParam(
        override val paramName: String,
        override val type: KSType,
        override val isNullable: Boolean,
    ) : ParamBinding {
        override val wireName: String get() = paramName
    }

    data class HeaderMapParam(
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
    val wrapped: Boolean,
    val skipAuth: Boolean,
    val multipart: Boolean,
    val staticHeaders: List<Pair<String, String>>,
    val paging: PagingConfig?,
)

internal data class ApiInterface(
    val declaration: KSClassDeclaration,
    val packageName: String,
    val simpleName: String,
    val functions: List<ApiFunction>,
)

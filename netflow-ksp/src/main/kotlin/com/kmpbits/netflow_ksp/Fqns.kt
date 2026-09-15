package com.kmpbits.netflow_ksp

internal object Fqns {
    const val NET_FLOW_API = "com.kmpbits.netflow_annotations.NetFlowApi"
    const val GET = "com.kmpbits.netflow_annotations.GET"
    const val POST = "com.kmpbits.netflow_annotations.POST"
    const val PUT = "com.kmpbits.netflow_annotations.PUT"
    const val DELETE = "com.kmpbits.netflow_annotations.DELETE"
    const val PATCH = "com.kmpbits.netflow_annotations.PATCH"
    const val PATH = "com.kmpbits.netflow_annotations.Path"
    const val QUERY = "com.kmpbits.netflow_annotations.Query"
    const val HEADER = "com.kmpbits.netflow_annotations.Header"
    const val BODY = "com.kmpbits.netflow_annotations.Body"
    const val MULTIPART = "com.kmpbits.netflow_annotations.Multipart"
    const val PART = "com.kmpbits.netflow_annotations.Part"
    const val FILE_PART = "com.kmpbits.netflow_core.builders.FilePart"
    const val SERIALIZABLE = "kotlinx.serialization.Serializable"
    const val URL = "com.kmpbits.netflow_annotations.Url"
    const val QUERY_MAP = "com.kmpbits.netflow_annotations.QueryMap"
    const val HEADER_MAP = "com.kmpbits.netflow_annotations.HeaderMap"
    const val HEADERS = "com.kmpbits.netflow_annotations.Headers"
    const val WRAPPED = "com.kmpbits.netflow_annotations.Wrapped"
    const val SKIP_AUTH = "com.kmpbits.netflow_annotations.SkipAuth"
    const val PAGINATED = "com.kmpbits.netflow_annotations.Paginated"
    const val PAGING_DATA = "androidx.paging.PagingData"
    const val PAGING_MODEL = "com.kmpbits.netflow_paging.model.PagingModel"
    const val EXPERIMENTAL_PAGING_API = "androidx.paging.ExperimentalPagingApi"
    const val PAGING_DESERIALIZABLES_PKG = "com.kmpbits.netflow_paging.deserializable"
    const val NET_FLOW_CALL = "com.kmpbits.netflow_core.request.NetFlowCall"

    val HTTP_METHOD_ANNOTATIONS = setOf(GET, POST, PUT, DELETE, PATCH)

    const val FLOW = "kotlinx.coroutines.flow.Flow"
    const val RESULT_STATE = "com.kmpbits.netflow_core.states.ResultState"
    const val ASYNC_STATE = "com.kmpbits.netflow_core.states.AsyncState"
    const val LIST = "kotlin.collections.List"
    const val MAP = "kotlin.collections.Map"
    const val STRING = "kotlin.String"
    const val UNIT = "kotlin.Unit"

    const val CLIENT_PKG = "com.kmpbits.netflow_core.client"
    const val ENUMS_PKG = "com.kmpbits.netflow_core.enums"
    const val DESERIALIZABLES_PKG = "com.kmpbits.netflow_core.deserializables"
}

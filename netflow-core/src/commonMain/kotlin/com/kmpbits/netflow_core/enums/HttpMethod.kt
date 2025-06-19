package com.kmpbits.netflow_core.enums

sealed interface HttpMethod {
    data object Get : HttpMethod
    data object Post : HttpMethod
    data object Put : HttpMethod
    data object Delete : HttpMethod
    data object Patch : HttpMethod
    data object Head : HttpMethod
}
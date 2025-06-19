package com.kmpbits.netflow_core.enums

sealed interface ErrorResponseType {
    data object Http : ErrorResponseType
    data object Empty : ErrorResponseType
    data object Timeout : ErrorResponseType
    data object ServiceUnavailable : ErrorResponseType
    data object Unknown : ErrorResponseType
    data object InternetConnection : ErrorResponseType
}
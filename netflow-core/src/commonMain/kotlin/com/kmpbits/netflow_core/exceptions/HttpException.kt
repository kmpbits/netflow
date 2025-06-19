package com.kmpbits.netflow_core.exceptions

class HttpException(
    val code: Int,
    override val message: String?
): IllegalStateException(message)
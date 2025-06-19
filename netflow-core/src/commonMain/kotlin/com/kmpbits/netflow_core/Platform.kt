package com.kmpbits.netflow_core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
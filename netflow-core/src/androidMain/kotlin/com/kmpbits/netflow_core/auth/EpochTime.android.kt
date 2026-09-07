package com.kmpbits.netflow_core.auth

internal actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L

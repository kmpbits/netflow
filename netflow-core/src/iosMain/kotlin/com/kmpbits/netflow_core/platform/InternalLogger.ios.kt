package com.kmpbits.netflow_core.platform

import platform.Foundation.NSLog

internal actual fun netflowLogger(message: String) {
    NSLog(message)
}
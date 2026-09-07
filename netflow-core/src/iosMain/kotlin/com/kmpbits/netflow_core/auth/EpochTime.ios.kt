package com.kmpbits.netflow_core.auth

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentEpochSeconds(): Long = NSDate().timeIntervalSince1970.toLong()

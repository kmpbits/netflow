package com.kmpbits.netflow_core.platform

import android.util.Log

internal actual fun netflowLogger(message: String) {
    Log.d("NetFlow", message)
}
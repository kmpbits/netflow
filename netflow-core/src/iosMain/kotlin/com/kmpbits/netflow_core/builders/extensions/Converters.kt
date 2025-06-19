package com.kmpbits.netflow_core.builders.extensions

import com.kmpbits.netflow_core.exceptions.NetFlowException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.refTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.posix.memcpy

internal fun Map<String, Any>.toJsonString(): String {
    val jsonMap = this.mapValues { (_, value) ->
        when (value) {
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString()) // fallback
        }
    }

    return Json.encodeToString(JsonObject(jsonMap))
}

internal fun String.toNSData(): NSData {
    return NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)!!
}

// Converts NSData to ByteArray
@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    memcpy(bytes.refTo(0), this.bytes, size.convert())
    return bytes
}

// Converts NSError to Throwable (you can customize this further)
internal fun NSError.toThrowable(): Throwable = NetFlowException(localizedDescription)
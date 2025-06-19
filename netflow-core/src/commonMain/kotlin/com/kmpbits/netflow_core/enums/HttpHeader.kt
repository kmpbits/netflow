package com.kmpbits.netflow_core.enums

class HttpHeader private constructor(
    val header: String
) {

    companion object {
        val ACCEPT = HttpHeader("Accept")
        val ACCEPT_CHARSET = HttpHeader("Accept-Charset")
        val ACCEPT_ENCODING = HttpHeader("Accept-Encoding")
        val ACCEPT_LANGUAGE = HttpHeader("Accept-Language")
        val ACCEPT_RANGES = HttpHeader("Accept-Ranges")
        val AGE = HttpHeader("Age")
        val ALLOW = HttpHeader("Allow")

        // Application-Layer Protocol Negotiation, HTTP/2
        val AUTHENTICATION_INFO = HttpHeader("Authentication-Info")
        val AUTHORIZATION = HttpHeader("Authorization")
        val CACHE_CONTROL = HttpHeader("Cache-Control")
        val CONNECTION = HttpHeader("Connection")
        val CONTENT_ENCODING = HttpHeader("Content-Encoding")
        val CONTENT_LANGUAGE = HttpHeader("Content-Language")
        val CONTENT_LENGTH = HttpHeader("Content-Length")
        val CONTENT_LOCATION = HttpHeader("Content-Location")
        val CONTENT_RANGE = HttpHeader("Content-Range")
        val CONTENT_TYPE = HttpHeader("Content-Type")

        fun custom(header: String) = HttpHeader(header)
    }
}

package com.kmpbits.netflow_annotations

/** Marks an interface whose functions NetFlow's KSP processor turns into an implementation. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class NetFlowApi

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class GET(val path: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class POST(val path: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PUT(val path: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class DELETE(val path: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PATCH(val path: String)

/** Binds a function parameter to a `{name}` placeholder in the path. Empty [name] = use the parameter name. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Path(val name: String = "")

/** Binds a function parameter to a URL query parameter. Empty [name] = use the parameter name. A null value omits it. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Query(val name: String = "")

/** Binds a function parameter to a request header. Empty [name] = use the parameter name. A null value omits it. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Header(val name: String = "")

/** Binds a function parameter as the request body. Any `@Serializable` type, or `Map<String, Any>`. At most one per function. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Body

/** Method-level static headers. Each entry is `"Name: Value"` (split on the first `:`). */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Headers(vararg val value: String)

/**
 * Route this function's response through the `responseWrapped*` family — for APIs that
 * return `{ "data": ... }` instead of a plain object/array.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Wrapped

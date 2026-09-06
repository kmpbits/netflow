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

/** Binds a function parameter as the request body. v1 accepts only `Map<String, *>`. At most one per function. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Body

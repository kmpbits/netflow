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
 * Send this function's request as `multipart/form-data`. Required on any function
 * that has a [Part] parameter. Cannot be combined with [Body]. Use with POST, PUT
 * or PATCH.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Multipart

/**
 * Binds a function parameter to one part of a [Multipart] request body.
 *
 * Supported parameter types:
 * - `FilePart` — a file part (`filename` comes from the value).
 * - `String` / `Int` / `Long` / `Double` / `Boolean` — a text field.
 * - any `@Serializable` type — serialized to JSON, sent with `Content-Type: application/json`.
 *
 * Empty [name] = use the parameter name. A nullable parameter with a null value omits the part.
 * [filename] is reserved for a future raw-`ByteArray` form and is currently unused.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class Part(val name: String = "", val filename: String = "")

/**
 * Route this function's response through the `responseWrapped*` family — for APIs that
 * return `{ "data": ... }` instead of a plain object/array.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Wrapped

/**
 * Opt this function out of automatic auth (see the client's `auth { }` block): no
 * `Authorization` header is attached and a `401` is returned as-is, without a token
 * refresh. Use it for login, sign-up, and token-refresh endpoints.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class SkipAuth

/**
 * Optional configuration for a `Flow<PagingData<T>>` function. The paged call is
 * network-only (`onlyApiCall = true`); there is no local cache. Remote + local
 * paging stays on the `call {}` DSL.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Paginated(
    val pageQueryName: String = "page",
    val pageSize: Int = 20,
)

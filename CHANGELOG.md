# Changelog

## [0.12.0]

### New features
- **`netflow-ksp` — `@Url`, `@QueryMap`, `@HeaderMap`.** `@GET`/`@POST`/`@PUT`/`@DELETE`/`@PATCH` now default their `path` to `""`, so a bare `@GET` reads naturally alongside `@Url`. `@Url` on a `String` parameter replaces the request's full URL (base URL + path), skipping `baseUrl` when the value is already an absolute `http://`/`https://` URL; it requires an empty method path and is incompatible with `@Path`. `@QueryMap` / `@HeaderMap` bind a `Map<String, Any?>` parameter as dynamic query parameters / headers on top of any individual `@Query`/`@Header` parameters — a `null` map omits all entries, a `null` value omits that one entry. At most one `@Url`, one `@QueryMap`, and one `@HeaderMap` per function.
- **`netflow-core`.** `createUrl` now returns an already-absolute `http://`/`https://` path unchanged instead of prefixing it with `baseUrl` — the mechanism `@Url` relies on, also usable directly from the `call {}` DSL by setting `path` to a full URL. `NetFlowMockRequest` gained a `parameters` field so tests can assert on query parameters added via `parameter(...)`, `@Query`, or `@QueryMap`.

## [0.11.0]

### New features
- **`netflow-core` — multipart requests.** A `multipart { }` block on `call { }` sends a `multipart/form-data` body: `part(name, value)` / `part(name, value, contentType)` for text fields, `filePart(name, filename, bytes, contentType)` (or `filePart(name, FilePart(...))`) for files, and `jsonPart(name, value)` for a `@Serializable` value sent as JSON. Bytes are held in memory (`ByteArray`). Cannot be combined with `body(...)`; requires POST, PUT or PATCH. On Android it maps to OkHttp's `MultipartBody`; on iOS the body is assembled as `NSData` by hand. `MockNetFlowClient` records the parts on `NetFlowMockRequest.parts`.
- **`netflow-ksp` — `@Multipart` / `@Part`.** A function annotated `@Multipart` with `@Part` parameters generates the `multipart { }` block. `@Part` accepts `FilePart`, primitives (`String`/`Int`/`Long`/`Double`/`Boolean`), and any `@Serializable` type (sent as JSON). A nullable `@Part` with a null argument omits the part. `@Multipart` cannot be combined with `@Body`, and `@Part` requires `@Multipart` on the function — both are compile-time errors.

## [0.10.0]

### Breaking changes
- **`NetFlowResponse.headers` now holds the response's headers.** Up to 0.9.0 this field mistakenly held the *request*'s headers instead. Code relying on the old value needs to read the request's headers elsewhere.

### New features
- **`netflow-core` — interceptors.** `addInterceptor(...)` on `netflowClient { }` registers a `NetFlowInterceptor` written once in `commonMain` that runs on both engines:
  - `chain.request` gives you an immutable `InterceptedRequest` (method, url, headers, body); `newBuilder()` lets you change the url and headers;
  - not calling `chain.proceed(...)` short-circuits the chain and returns a response without touching the network;
  - registration order is execution order; it runs inside the retry loop and after auth, so once per attempt and already with the final `Authorization` header;
  - `MockNetFlowClient` runs the same chain, so interceptors are testable without a network call.
- **`netflow-core` — certificate pinning.** `pinning { pin("api.example.com", "sha256/…", "sha256/…") }` maps to `CertificatePinner` on Android and to SPKI verification in an `NSURLSessionDelegate` on iOS. Multiple pins per host (always declare a backup for rotation), `*.host` for one subdomain level, validation at client construction, and a pin failure fails the request. Hosts with no pin declared are unaffected.
- **`netflow-core` — `followRedirects`.** Configurable on `netflowClient { }`, `false` by default on both platforms. Up to 0.9.0, Android did not follow redirects while iOS did.

### Fixes
- **`netflow-core` (Android)** — removed `CustomHeaderInterceptor`, which re-applied the client's headers after the request already had them, and would have resurrected headers removed by an interceptor.
- **`netflow-core`** — `NetFlowResponse`'s constructor is public, so an interceptor can return a synthetic response.

## [0.9.0]

### New features
- **`netflow-ksp`** — a `suspend` annotated function may return a bare model type (`T` or `List<T>`), generating `responseToModel<T>()`: the deserialized value, or an `HttpException` on a non-2xx response (Retrofit-style). `@Wrapped` and `Unit` are not supported on this shape.
- **`netflow-ksp`** — a `NetFlowCall` return may now be declared on a `suspend` function. The modifier is redundant (`prepareCall { }` is synchronous), but it lets an interface keep a uniformly `suspend` surface.

## [0.8.0]

### New modules
- **`netflow-token-storage`** — persistent `TokenStorage` for auth. `SettingsTokenStorage(settings, key)` stores the token pair as one JSON blob over a `multiplatform-settings` `Settings`; ships no encryption (back it with the iOS Keychain / Android `EncryptedSharedPreferences`).

### New features
- **`netflow-core` — bearer authentication.** Configure `auth { }` once on the client:
  - automatic `Authorization` header on every request from `loadTokens { }` (or a `storage(...)`);
  - on `401`, a single-flight `refreshTokens { }` call + one retry — N concurrent 401s trigger exactly one refresh;
  - `refreshTokens { }` receives an auth-free `RawNetFlowClient` (also a `NetFlowClient`, so a generated `@NetFlowApi` refresh interface works there) and returns `BearerTokens?` — `null` ends the session;
  - `authState: StateFlow<AuthState>` (`Unknown` / `Authenticated` / `Unauthenticated`), plus `setTokens()` / `clearTokens()`;
  - `RequestBuilder.skipAuth()` to opt a request out (login / sign-up / refresh / public endpoints);
  - works across Flow, Async, Paging, and `@NetFlowApi` interfaces — they all funnel through `NetFlowRequest.response()`.
  - No `auth { }` block ⇒ behaviour is byte-identical to 0.7.0.
- **`netflow-core` — `TokenStorage`.** `auth { storage(...) }` seeds from `load()` on the first request and writes back automatically on every token change and on session end. Storage failures are swallowed — the in-memory token stays valid. `loadTokens { }` still works and wins for seeding. `InMemoryTokenStorage()` included.
- **`netflow-core` — proactive refresh.** `auth { refreshLeeway = 30.seconds }` reads the JWT `exp` claim (no signature check) and refreshes *before* sending when the token is near expiry, skipping the wasted 401 round-trip. Opaque tokens, a missing `exp`, or clock skew fall back to the reactive path. Wall-clock time is platform-provided (`System.currentTimeMillis` / `NSDate`) — no new dependency.
- **`netflow-annotations` — `@SkipAuth`.** Method-level annotation; `netflow-ksp` emits `skipAuth()` into the generated `call { }` block so login / refresh endpoints declared as `@NetFlowApi` methods opt out of the auth interceptor.
- **`MockNetFlowClient`** — new `auth = { ... }` constructor parameter (same config as the real client), `NetFlowMockResponse.unauthorized()`, `MockResponseQueue` for sequential responses, and a case-insensitive `NetFlowMockRequest["Header-Name"]` accessor. The full refresh dance is testable with no network.

## [0.7.0]

### New modules
- **`netflow-annotations`** — Retrofit-style annotation set (`@NetFlowApi`, `@GET`/`@POST`/`@PUT`/`@DELETE`/`@PATCH`, `@Path`, `@Query`, `@Header`, `@Body`, `@Headers`, `@Wrapped`).
- **`netflow-ksp`** — KSP processor that generates a `NetFlowClient.create<Name>()` extension and an implementation delegating to the `call {}` DSL. Supports `Flow<ResultState<T>>`, `AsyncState<T>`, and their `List<T>` variants; parameter-name inference with wire-name overrides; null-omitted `@Query`/`@Header`; typed `@Body` (any `@Serializable` type or `Map<String, Any>`); method-level `@Headers`; `@Wrapped` routing to the `responseWrapped*` family; `Flow<PagingData<T>>` returns generate a network-only `responsePaginated` call (`onlyApiCall = true`), with optional `@Paginated(pageQueryName, pageSize)`. DTO → domain mapping uses the existing `ResultState` / `AsyncState` / `Flow` / `PagingData` `.map` helpers — annotations carry no `transform` parameter by design.

### New features
- **`netflow-core`** — new reified `RequestBuilder.body(value: T)` overload for arbitrary `@Serializable` request bodies, also usable from the hand-written `call {}` DSL.
- **`netflow-core`** — `NetFlowCall` + `NetFlowClient.prepareCall { }`: build a request and compose the response (`responseFlow` / `responseAsync` / `responsePaginated`, including `local {}` / `onNetworkSuccess`) separately. Annotated methods may return `NetFlowCall` when the repository owns the response side.

## [0.6.0]

### New features
- **Simplified API** — `responseFlow`, `responseAsync`, and paging variants now accept `transform` as a primary parameter when mapping between `ApiType` and `DisplayType`. This improves compiler safety and simplifies the call site.
- **`localQuery`** — New configuration for `responsePaginated` that allows defining remote+local paging using raw lambdas (`countQuery`, `itemsQuery`, and `invalidation` flow), eliminating the need for custom `PagingSource` implementations.
- **New Response Extensions** — Added dedicated extensions for common response patterns:
    - `responseWrappedFlow` / `responseWrappedAsync` for `{ "data": ... }` envelopes.
    - `responseListFlow` / `responseListAsync` for top-level arrays.
    - `responseWrappedListFlow` / `responseWrappedListAsync` for `{ "data": [...] }`.
- **Automatic Paging Invalidation** — `localSource` and `localSourceLong` wrappers now automatically register invalidation callbacks on the provided `PagingSource`, ensuring UI updates when the local database changes.
- **Better Paging Keys** — Improved `getRefreshKey` implementation in paging wrappers to provide smoother list jumping and restoration.
---

## [0.2.0]

### New modules
- **`netflow-paging`** — Jetpack Paging 3 integration via `responsePaginated`. Supports network-only and remote+local strategies with `RemoteMediator`.
- **`PagingCollectionViewController`** — iOS bridge for paging data, exposing `loadStateFlow`, `onPagesUpdatedFlow`, `getItems()`, and `loadNextPage()` via SKIE.

### New features
- **`MockNetFlowClient`** — drop-in `NetFlowClient` for tests. No real network calls, supports response delays, request recording, and assertion helpers (`assertCalled`, `assertCalledTimes`, `assertNotCalled`).
- **`localSourceLong`** — `localSource` overload for `PagingSource<Long, E>`, enabling direct SQLDelight `QueryPagingSource` integration.
- **`onlyLocalCall`** — skip the network entirely and read only from the local source.
- **`wrappedResponse`** — built-in support for `{ "data": ... }` envelope APIs on both `responseFlow` and `responseAsync`.
- **Retry** — configurable `times`, `delay`, and `retryOn` condition per request.
- **Custom headers** — per-request headers via `header()`.

### Bug fixes
- `responseAsync<Unit, Unit>` no longer fails on 200 responses with no body (e.g. DELETE endpoints).
- `isSuccess` now correctly returns `true` for any 2xx response regardless of body presence.

---

## [0.1.0] — Initial release

- Kotlin Multiplatform support (Android and iOS).
- `responseFlow` — Flow-based requests with `ResultState` (Loading, Success, Error).
- `responseAsync` — suspending one-shot requests with `AsyncState`.
- Two-type API `<ApiType, DisplayType>` — map inside the builder, no `.map()` at the call site.
- Local cache integration with observation support.
- Debug logging with configurable levels (None, Basic, Headers, Body).
- Native HTTP clients — OkHttp on Android, URLSession on iOS.

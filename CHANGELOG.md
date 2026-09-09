# Changelog

## [0.10.0]

### Breaking changes
- **`NetFlowResponse.headers` passa a conter os headers da resposta.** Até 0.9.0 este campo continha, por engano, os headers do *pedido*. Código que dependesse do valor antigo tem de ler os headers do pedido noutro sítio.

### New features
- **`netflow-core` — interceptors.** `addInterceptor(...)` no `netflowClient { }` regista um `NetFlowInterceptor` escrito uma vez no `commonMain` e que corre nos dois motores:
  - `chain.request` dá um `InterceptedRequest` imutável (método, url, headers, corpo); `newBuilder()` permite alterar url e headers;
  - não chamar `chain.proceed(...)` faz curto-circuito e devolve uma resposta sem tocar na rede;
  - a ordem de registo é a ordem de execução; corre dentro do ciclo de retry e depois do auth, portanto uma vez por tentativa e já com o `Authorization` final;
  - o `MockNetFlowClient` corre a mesma cadeia, por isso os interceptors testam-se sem rede.
- **`netflow-core` — certificate pinning.** `pinning { pin("api.exemplo.com", "sha256/…", "sha256/…") }` traduz para `CertificatePinner` no Android e para verificação SPKI num `NSURLSessionDelegate` no iOS. Vários pins por host (declara sempre um de backup para a rotação), `*.host` para um nível de subdomínio, validação na construção do cliente, e falha do pin faz o pedido falhar. Hosts sem pin declarado não são afectados.
- **`netflow-core` — `followRedirects`.** Configurável no `netflowClient { }`, `false` por omissão nas duas plataformas. Até 0.9.0 o Android não seguia redirects e o iOS seguia.

### Fixes
- **`netflow-core` (Android)** — removido o `CustomHeaderInterceptor`, que voltava a aplicar os headers do cliente depois de o pedido já os ter, e que ressuscitaria headers removidos por um interceptor.
- **`netflow-core`** — o construtor de `NetFlowResponse` é público, para que um interceptor possa devolver uma resposta sintética.

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

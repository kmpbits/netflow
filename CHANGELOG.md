# Changelog

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

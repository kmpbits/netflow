# NetFlow KMP
A networking layer for Kotlin Multiplatform: one client, one state model, and one testing story across plain API calls, local-cache / offline-first flows, and Jetpack Paging 3.

Start with **Retrofit-style annotated interfaces** for your straightforward endpoints — then drop to the `call {}` DSL on the *same* client, for the *same* API, when an endpoint needs caching, offline reads, or paging. The annotations are the familiar front door; the DSL is the engine behind it.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/netflow-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kmpbits/netflow-core)
[![Tests](https://github.com/kmpbits/netflow/actions/workflows/test.yml/badge.svg)](https://github.com/kmpbits/netflow/actions/workflows/test.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Features

- Kotlin Multiplatform support (Android and iOS)
- **Annotated interfaces** (Retrofit-style, KSP-generated, no reflection) as the on-ramp — and the `call {}` DSL for everything annotations can't express
- Multiple response strategies:
  - Flow (with UI state handling)
  - Async (suspending, one-shot)
  - Paginated (Jetpack Paging 3 via `netflow-paging`)
- Two-type API: separate deserialization type (`ApiType`) from display type (`DisplayType`) — no trailing `.map` needed
- `wrappedResponse` flag for APIs that return `{ "data": ... }` envelopes
- **Bearer auth** — automatic token attach, single-flight `401 → refresh → retry`, optional proactive refresh from the JWT `exp`, `authState` flow, pluggable `TokenStorage`
- `multipart/form-data` uploads — `multipart { }` on the DSL, `@Multipart` / `@Part` on annotated interfaces
- Local cache integration with observation support
- Built-in error handling
- Debug logging with multiple levels (None, Basic, Headers, Body)
- `MockNetFlowClient` for testing — no real network calls, with response delays and request history
- iOS paging support via `PagingCollectionViewController`

---

## Installation

### Core module

```kotlin
dependencies {
    implementation("io.github.kmpbits:netflow-core:<latest_version>")
}
```

### Paging module (optional)

Adds `responsePaginated` with Jetpack Paging 3 support.

```kotlin
dependencies {
    implementation("io.github.kmpbits:netflow-core:<latest_version>")
    implementation("io.github.kmpbits:netflow-paging:<latest_version>")
}
```

Check the latest versions on [Maven Central](https://central.sonatype.com/artifact/io.github.kmpbits/netflow-core).

### Token storage module (optional)

Adds `SettingsTokenStorage` for persisting auth tokens (see [Authentication](#authentication)).

```kotlin
dependencies {
    implementation("io.github.kmpbits:netflow-core:<latest_version>")
    implementation("io.github.kmpbits:netflow-token-storage:<latest_version>")
}
```

### Annotations module (optional)

Declare your API as an annotated interface (Retrofit-style) and let a KSP
processor generate the implementation. Works on all Kotlin Multiplatform
targets — no runtime reflection.

**This is the on-ramp, not a separate library.** Use annotations for the plain
request/response endpoints — the 80% case. The generated interface returns the
same `ResultState` / `AsyncState` / `PagingData` types the DSL uses, runs on the
same `NetFlowClient`, and is tested with the same `MockNetFlowClient`. When an
endpoint needs local caching, offline reads, `onNetworkSuccess` side effects, or
remote+local paging, write that one method with `client.call { … }` — nothing
else changes. You never juggle two HTTP stacks or two state models.

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("io.github.kmpbits:netflow-core:<latest_version>")
    implementation("io.github.kmpbits:netflow-annotations:<latest_version>")
    add("kspCommonMainMetadata", "io.github.kmpbits:netflow-ksp:<latest_version>")
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
}
```

```kotlin
@NetFlowApi
interface TodoApi {

    @GET("todos")
    suspend fun getTodos(@Query completed: Boolean?): AsyncState<List<TodoDto>>

    @GET("todos/{id}")
    fun observeTodo(@Path id: Int): Flow<ResultState<TodoDto>>

    @Headers("Accept: application/json")
    @POST("todos")
    suspend fun create(@Body request: CreateTodoRequest): AsyncState<TodoDto>

    @Wrapped
    @GET("todos/{id}")
    suspend fun get(@Path id: Int): AsyncState<TodoDto>

    @GET("todos")
    fun pagedTodos(): Flow<PagingData<TodoDto>>

    @GET("todos")
    fun todosCall(): NetFlowCall     // request only — compose the response yourself

    @Multipart
    @POST("todos/{id}/attachments")
    suspend fun upload(
        @Path id: Int,
        @Part("caption") caption: String,
        @Part("file") file: FilePart,
    ): AsyncState<Unit>

    @GET
    suspend fun download(@Url url: String): AsyncState<ByteArray>

    @GET("todos")
    suspend fun search(
        @QueryMap filters: Map<String, Any?>?,
        @HeaderMap extraHeaders: Map<String, Any?>?,
    ): AsyncState<List<TodoDto>>
}

val api = client.createTodoApi()   // generated extension on NetFlowClient
```

**Supported:** `@GET` / `@POST` / `@PUT` / `@DELETE` / `@PATCH` (path defaults to
`""`, for use with `@Url`); `@Path`, `@Query`, `@Header`, `@Body`, `@Multipart` +
`@Part`, `@Url`, `@QueryMap` / `@HeaderMap`; method-level `@Headers("Name: Value", ...)`;
`@Wrapped` for `{ "data": ... }` envelope responses; `@SkipAuth` to opt a method
out of the client's `auth { }` (login / sign-up / refresh endpoints). `@Body` accepts any
`@Serializable` type or `Map<String, Any>`. `@Multipart` sends a `multipart/form-data` body;
each `@Part` is a `FilePart` (file), a primitive (text field), or a `@Serializable` value
(JSON field), and a null `@Part` is omitted. An optional `@Progress` parameter
(`(Long, Long) -> Unit`) reports upload byte progress; it requires `@Multipart`.
`@Multipart` and `@Body` are mutually exclusive.
`@Url` on a `String` parameter replaces the full request URL — it needs an empty method path
and no `@Path` on the same function. `@QueryMap` / `@HeaderMap` bind a `Map<String, Any?>` as
dynamic query parameters / headers on top of any individual `@Query`/`@Header`; a null map
omits everything, a null value omits that entry. Return types `Flow<ResultState<T>>`
and `Flow<ResultState<List<T>>>` (non-suspend), `AsyncState<T>` and
`AsyncState<List<T>>` (suspend), a bare `T` or `List<T>` (suspend, Retrofit-style:
returns the value or throws `HttpException`), `Flow<PagingData<T>>` (non-suspend,
network-only paging), and `NetFlowCall` (the request without a response strategy,
`suspend` or not, compose it in the repository). `@Query` / `@Header` names default to the parameter name and
take an override string (`@Query("user_id") userId: Int`); a null `@Query` /
`@Header` value is omitted from the request.

**Not yet supported:** the `onNetworkSuccess` / `local {}` cache hooks (including
remote+local paging). Use the `call {}` DSL directly for those.

### Mapping to domain types

Annotated functions return the API DTO. Map to your domain model in the
repository with the `map` helpers from `netflow-core` — `AsyncState.map`,
`ResultState.map`, and `Flow.map`:

```kotlin
class TodoRepository(client: NetFlowClient) {

    private val api = client.createTodoApi()

    suspend fun getTodos(): AsyncState<List<Todo>> =
        api.getTodos(completed = null).map { dtos -> dtos.map { it.toModel() } }

    fun observeTodo(id: Int): Flow<ResultState<Todo>> =
        api.observeTodo(id).map { state -> state.map { it.toModel() } }
}
```

This is deliberate: the two-type `transform` stays one layer out of the
annotations, so the mapping is always explicit and compiler-checked — the same
guarantee the `call {}` DSL gives with its required `transform` parameter.

### Paging

A function returning `Flow<PagingData<T>>` (where `T` extends `PagingModel`)
generates a **network-only** paged call. `@Paginated(pageQueryName, pageSize)`
overrides the defaults (`"page"`, `20`).

```kotlin
@GET("posts")
fun pagedPosts(@Query tag: String?): Flow<PagingData<PostDto>>
```

Map to domain in the repository with `PagingData.map`:

```kotlin
fun pagedPosts(tag: String?) = api.pagedPosts(tag).map { it.map { dto -> dto.toModel() } }
```

Remote + local paging (`RemoteMediator`, local `PagingSource`, insert/delete
callbacks) stays on the `call {}` DSL — `responsePaginated { localSource(...) }`.

### Composing the response yourself (`NetFlowCall`)

Return `NetFlowCall` and the generated method stops at the request. Finish it in
the repository with any `responseX` function — this is how you add a local cache
or `onNetworkSuccess` side effects to an annotated endpoint:

```kotlin
@GET("todos")
fun todos(): NetFlowCall

// repository
fun getTodos(): Flow<ResultState<Todo>> =
    api.todos().responseFlow<TodoDto, Todo>(transform = { it.toModel() }) {
        onNetworkSuccess { db.insertTodos(it) }
        local({ observe { db.todos() } }, transform = { it.toModel() })
    }
```

`@Wrapped` / `@Paginated` are not allowed on a `NetFlowCall` method — those are
choices you make on the `responseX` call. The method can be `suspend` or not; the
`responseX` you compose on the result is already suspending either way.
`client.prepareCall { … }` builds a `NetFlowCall` from the hand-written DSL too.

### Bare model return

A `suspend` function returning a plain type maps to `responseToModel<T>()`: you
get the deserialized value, or an `HttpException` on a non-2xx response. This is
the Retrofit default style, for code that prefers `try/catch` (or a global
handler) over a sealed state.

```kotlin
@GET("todos/{id}")
suspend fun getTodo(@Path id: Int): TodoDto

@GET("todos")
suspend fun getTodos(): List<TodoDto>
```

It returns the DTO, same as every other shape, so map to your domain type in the
repository. `@Wrapped` isn't supported here — use `AsyncState<T>` with `@Wrapped`
for envelope APIs, or return `NetFlowCall`. `Unit` isn't allowed either; use
`AsyncState<Unit>`.

---

## Getting started

### Initialize the client

```kotlin
val client = netflowClient {
    baseUrl = "https://api.example.com"

    header(Header(HttpHeader.custom("custom-header"), "value"))
    header(Header(HttpHeader.CONTENT_TYPE), "application/json")
}
```

### Basic request

```kotlin
val response = client.call {
    path = "/users"
    method = HttpMethod.Get
}.response()
```

### Deserialize to model

```kotlin
val user: User = client.call {
    path = "/users/1"
}.responseToModel<User>()
```

### Multipart upload

```kotlin
client.call {
    method = HttpMethod.Post
    path = "/todos/1/attachments"
    multipart {
        part("caption", "before")
        filePart("file", filename = "shot.png", bytes = imageBytes, contentType = "image/png")
        onProgress { sent, total -> println("$sent / $total") }
    }
}.response()
```

`multipart { }` holds each part's bytes in memory. It requires POST, PUT or PATCH
and cannot be combined with `body(...)`. `onProgress { sent, total -> }` is optional
and reports upload byte progress; it fires on whatever thread the platform delivers
it on, so hop to your UI thread yourself if you're updating UI from it.

---

## Working with Flow

### Same type — single type parameter, no transform needed

When your DTO and domain model are the same type, pass only one type parameter:

```kotlin
val flow = client.call {
    path = "/users/1"
}.responseFlow<UserDto>()
```

### Different types — transform is required

When `ApiType` and `DisplayType` differ, pass `transform` as the first argument. The compiler enforces this — forgetting it is a build error, not a runtime crash.

```kotlin
val flow = client.call {
    path = "/users/1"
}.responseFlow<UserDto, User>(transform = { it.toModel() })
```

### With local cache

```kotlin
val usersFlow = client.call {
    path = "/users"
    method = HttpMethod.Get
}.responseFlow<UserDto, User>(transform = { it.toModel() }) {
    onNetworkSuccess { dto ->
        queries.insertUser(dto.toEntity())
    }

    local({ observe { queries.getUser() } }, transform = { it.toModel() })
}
```

The `transform` inside `local()` maps from the database entity type directly to `DisplayType` — it drives what gets shown while the network call is in flight. The `transform` on the function maps the network `ApiType` to `DisplayType` once the response arrives.

### Offline-only

```kotlin
local({
    onlyLocalCall = true
    call { queries.getAllUsers() }
}, transform = { it.toModel() })
```

### Wrapped API responses

For APIs that return `{ "data": { ... } }` instead of a plain object:

```kotlin
// Same type
responseWrappedFlow<UserDto>()

// Different types
responseWrappedFlow<UserDto, User>(transform = { it.toModel() })
```

Or set `wrappedResponse = true` inside the builder when using `responseFlow`:

```kotlin
responseFlow<UserDto, User>(transform = { it.toModel() }) {
    wrappedResponse = true
}
```

### List variants

```kotlin
// Same type
responseListFlow<UserDto>()
responseWrappedListFlow<UserDto>()

// Different types
responseListFlow<UserDto, User>(transform = { it.toModel() })
responseWrappedListFlow<UserDto, User>(transform = { it.toModel() })
```

### Observing Flow

```kotlin
lifecycleScope.launch {
    usersFlow.collectLatest { state ->
        when (state) {
            is ResultState.Loading -> showLoading()
            is ResultState.Success -> showUsers(state.data)
            is ResultState.Error -> showError(state.error.message)
        }
    }
}
```

---

## Working with Async

For one-shot suspending calls (no observation needed).

### Same type

```kotlin
suspend fun deleteUser(id: Int): AsyncState<Unit> {
    return client.call {
        path = "users/$id"
        method = HttpMethod.Delete
    }.responseAsync<Unit> {
        onNetworkSuccess { queries.deleteUser(id) }
    }
}
```

### Different types — transform is required

```kotlin
suspend fun getUser(id: Int): AsyncState<User> {
    return client.call {
        path = "users/$id"
    }.responseAsync<UserDto, User>(transform = { it.toModel() })
}
```

### List variants

```kotlin
// Same type
responseListAsync<UserDto>()
responseWrappedListAsync<UserDto>()

// Different types
responseListAsync<UserDto, User>(transform = { it.toModel() })
responseWrappedListAsync<UserDto, User>(transform = { it.toModel() })
```

### Wrapped responses (not list)

```kotlin
// Same type
responseWrappedAsync<UserDto>()

// Different types
responseWrappedAsync<UserDto, User>(transform = { it.toModel() })
```

---

## Working with Paging (netflow-paging)

`responsePaginated` integrates Jetpack Paging 3, supporting both network-only and remote+local strategies.

Your API response model must implement `PagingModel`:

```kotlin
@Serializable
data class PostDto(
    val id: Int,
    val title: String,
    override var page: Int = 0,
    override var lastUpdatedTimestamp: Long = 0L
) : PagingModel()
```

### Network-only paging

```kotlin
fun getPosts(): Flow<PagingData<Post>> = client.call {
    path = "/posts"
}.responsePaginated<PostDto, Post> {
    onlyApiCall = true
    networkTransform { it.toModel() }
}
```

### Remote + local paging

There are two ways to configure the local data source.

---

#### Option A — `localQuery` (recommended, no custom PagingSource needed)

Pass `countQuery`, `itemsQuery`, and an `invalidation` flow. The library creates and manages the `PagingSource` internally. The `invalidation` flow triggers a reload whenever the underlying data changes — SQLDelight users pass `query.asFlow()`, Room users pass their `Flow<List<T>>`.

```kotlin
fun getPosts(): Flow<PagingData<Post>> = client.call {
    path = "/posts"
}.responsePaginated<PostDto, Post> {
    localQuery(
        countQuery   = { database.postQueries.countPosts().executeAsOne() },
        itemsQuery   = { limit, offset -> database.postQueries.selectPosts(limit, offset).executeAsList() },
        invalidation = database.postQueries.selectAllPosts().asFlow(),
        transform    = { it.toModel() }
    )

    deleteOnRefresh = false
    insertAll(transform = { it.toEntity() }) { posts ->
        database.postQueries.transaction {
            database.postQueries.deleteAll()
            posts.forEach { database.postQueries.insertPost(it) }
        }
    }

    firstItemDatabase(
        itemDatabase = { database.postQueries.getFirstPost().executeAsOneOrNull() },
        timestamp    = { it.lastUpdatedTimestamp }
    )
}
```

---

#### Option B — `localSource` / `localSourceLong` (custom PagingSource)

Use this when you need full control over how data is loaded locally. You provide your own `PagingSource<Int, E>` (or `PagingSource<Long, E>` via `localSourceLong`).

```kotlin
class PostPagingSource(private val database: AppDatabase) : PagingSource<Int, PostEntity>() {

    private val query = database.postQueries.selectPosts()

    private val listener = object : Query.Listener {
        override fun queryResultsChanged() {
            invalidate()
            query.removeListener(this)
        }
    }

    init {
        query.addListener(listener)
    }

    override fun getRefreshKey(state: PagingState<Int, PostEntity>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostEntity> {
        // your load implementation
    }
}
```

Then wire it up:

```kotlin
fun getPosts(): Flow<PagingData<Post>> = client.call {
    path = "/posts"
}.responsePaginated<PostDto, Post> {
    localSource(
        pagingSource = { PostPagingSource(database) },
        transform    = { it.toModel() }
    )
    // ...
}
```

For SQLDelight sources that use `Long` keys (e.g. `QueryPagingSource`), use `localSourceLong` instead — keys are bridged to `Int` internally.

---

### Important: PagingSource invalidation

When using a custom `PagingSource` (Option B), it is **critical** to register a listener on your database query to trigger invalidation. Without this, the UI will not update when data changes (e.g., after a network refresh or a local deletion).

If you are using **SQLDelight**, follow the pattern in the example above (and in the sample app's `TodoPagingSource`):
1.  Store the query in a property.
2.  Create a `Query.Listener` that calls `invalidate()` and removes itself.
3.  Add the listener in `init`.

This ensures that whenever the underlying data changes, the `PagingSource` is marked as invalid, and the `Pager` will create a new one and reload the data.

---

### PagingBuilder options

| Property | Default | Description |
|---|---|---|
| `defaultPageSize` | `20` | Items loaded per page |
| `pageQueryName` | `"page"` | URL query parameter name for the page number |
| `onlyApiCall` | `false` | `true` for network-only (no local DB) |
| `wrappedResponse` | `false` | `true` when API returns `{ "data": [...] }` |
| `deleteOnRefresh` | `true` | Clear local DB before inserting on `REFRESH`. Set to `false` when handling delete inside `insertAll` |
| `refresh` | `false` | Force refresh on start, ignoring cache timeout |
| `cacheTimeout` | `1 hour` | How long before re-fetching from the network |

### Consuming in the ViewModel

```kotlin
val posts = repository.getPosts().cachedIn(viewModelScope)
```

### Consuming in Compose (Android)

```kotlin
val posts = viewModel.posts.collectAsLazyPagingItems()

LazyColumn {
    items(count = posts.itemCount, key = posts.itemKey { it.id }) { index ->
        posts[index]?.let { PostItem(it) }
    }
}
```

### Consuming on iOS (SwiftUI)

`netflow-paging` ships `PagingCollectionViewController` — a KMP class that bridges paging data to Swift. It is designed to be used with [SKIE](https://skie.touchlab.co) for async sequence support.

**ViewModel (Swift)**

```swift
import netflowCore // or your KMP framework name

@MainActor
final class PostListViewModel: ObservableObject {
    private let viewModel = // your KMP ViewModel from DI

    @Published private(set) var posts: [Post] = []
    @Published private(set) var isLoading: Bool = false

    private let delegate = PagingCollectionViewController<Post>()

    init() {
        observeData()
        observeLoadStates()
        observePagingData()
    }

    func loadNextPage() { delegate.loadNextPage() }

    private func observePagingData() {
        Task {
            for await pagingData in viewModel.posts {
                delegate.submitData(pagingData: pagingData)
            }
        }
    }

    private func observeData() {
        Task {
            for await _ in delegate.onPagesUpdatedFlow {
                self.posts = delegate.getItems()
                self.isLoading = false
            }
        }
    }

    private func observeLoadStates() {
        Task {
            for await loadState in delegate.loadStateFlow {
                guard let loadState else { continue }
                switch loadState.refresh {
                case _ as Paging_commonLoadStateLoading:
                    self.isLoading = true
                default:
                    self.isLoading = false
                }
            }
        }
    }

    deinit { delegate.clearScope() }
}
```

**View (SwiftUI)**

```swift
struct PostListView: View {
    @StateObject private var viewModel = PostListViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading && viewModel.posts.isEmpty {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(viewModel.posts, id: \.id) { post in
                            PostItemView(post: post)
                                .onAppear {
                                    if post.id == viewModel.posts.last?.id {
                                        viewModel.loadNextPage()
                                    }
                                }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Posts")
        }
    }
}
```

---

## Testing with MockNetFlowClient

`MockNetFlowClient` implements `NetFlowClient` and intercepts all requests without making any real network calls. It supports response delays, request recording, and assertion helpers.

```kotlin
val mockClient = MockNetFlowClient { request ->
    when {
        request.path == "posts" && request.method == HttpMethod.Get ->
            NetFlowMockResponse.success("""[{"id":1,"title":"Hello","completed":false}]""")

        request.path.startsWith("posts/") && request.method == HttpMethod.Delete ->
            NetFlowMockResponse.success()

        request.path == "posts" && request.method == HttpMethod.Post ->
            NetFlowMockResponse.success("""{"id":101,"title":"New Post","completed":false}""")

        else -> NetFlowMockResponse.notFound()
    }
}
```

### Simulating delays

```kotlin
NetFlowMockResponse.success(
    body = """[...]""",
    delay = 2.seconds   // simulates slow network
)
```

### Simulating errors

```kotlin
NetFlowMockResponse.error(code = 401, errorBody = "Unauthorized")
NetFlowMockResponse.serverError("Something went wrong")
NetFlowMockResponse.notFound()
```

### Asserting calls

```kotlin
// Called at least once
mockClient.assertCalled("posts", HttpMethod.Get)

// Called exactly N times
mockClient.assertCalledTimes("posts/1", HttpMethod.Delete, times = 1)

// Never called
mockClient.assertNotCalled("posts", HttpMethod.Post)
```

### Inspecting recorded requests

```kotlin
val request = mockClient.recordedRequests.first()
assertEquals(HttpMethod.Post, request.method)
assertEquals(mapOf("title" to "New Post"), request.body)

mockClient.clearRecordedRequests()
```

### Using with a repository

```kotlin
@Test
fun `delete removes item from local database`() = runTest {
    val mockClient = MockNetFlowClient { _ -> NetFlowMockResponse.success() }
    val repository = PostRepositoryImpl(mockClient, database)

    repository.deletePost(id = 1)

    mockClient.assertCalled("posts/1", HttpMethod.Delete)
}
```

### MockNetFlowResponse helpers

| Helper | Code | Description |
|---|---|---|
| `NetFlowMockResponse.success(body)` | `200` | Successful response with optional body |
| `NetFlowMockResponse.error(code, errorBody)` | custom | Client error |
| `NetFlowMockResponse.notFound()` | `404` | Not found |
| `NetFlowMockResponse.serverError(errorBody)` | `500` | Server error |

All helpers accept an optional `delay: Duration` parameter.

---

## Authentication

Configure `auth { }` once on the client. NetFlow adds the bearer token to every
request. On a `401` it calls your refresh block once, retries the failed request,
and holds a lock while it does, so ten requests that 401 at the same time trigger
one refresh instead of ten. You write the two lambdas. NetFlow decides when to
call them.

```kotlin
val client = netflowClient {
    baseUrl = "https://api.example.com"
    auth {
        loadTokens { tokenStore.read() }               // BearerTokens?  (seed, once)
        refreshTokens { raw ->                          // called on 401
            val res = raw.call {
                path = "auth/refresh"
                method = HttpMethod.Post
                body(RefreshRequest(refreshToken))      // refreshToken from the receiver
            }.responseAsync<TokenResponse>()
            when (res) {
                is AsyncState.Success -> BearerTokens(res.data.access, res.data.refresh)
                else -> null                            // null => AuthState.Unauthenticated
            }
        }
    }
}

client.authState          // StateFlow<AuthState>: Unknown | Authenticated | Unauthenticated
client.setTokens(tokens)  // after login
client.clearTokens()      // on logout

client.call { path = "public"; skipAuth() }   // opt a request out
```

`raw` is an auth-free client, so a refresh call can't recurse into another
refresh. It's a `NetFlowClient` too, so a generated `@NetFlowApi` interface works
there:

```kotlin
refreshTokens { raw ->
    when (val res = raw.createAuthApi().refresh(RefreshRequest(refreshToken!!))) {
        is AsyncState.Success -> BearerTokens(res.data.accessToken, res.data.refreshToken)
        else -> null
    }
}
```

### Annotated interfaces

Generated `@NetFlowApi` implementations run on the same client, so the token
attach and refresh-and-retry apply to them too. Put `@SkipAuth` on the endpoints
that don't need a token (login, sign-up, refresh) so the generated code doesn't
attach one or try to refresh on their 401:

```kotlin
@NetFlowApi
interface AuthApi {
    @SkipAuth @POST("auth/login")   suspend fun login(@Body body: LoginRequest): AsyncState<TokenResponse>
    @SkipAuth @POST("auth/refresh") suspend fun refresh(@Body body: RefreshRequest): AsyncState<TokenResponse>
}
```

### Proactive refresh

By default the refresh is reactive: send the request, refresh on `401`. Set
`refreshLeeway` and NetFlow also refreshes *before* sending, once the JWT is close
to expiring, so you skip the failed round-trip:

```kotlin
auth {
    refreshLeeway = 30.seconds     // refresh if the token expires within 30s
    refreshTokens { /* ... */ }
}
```

It reads the token's `exp` claim (no signature check, that's the server's job). If
the token isn't a JWT, has no `exp`, or the device clock is off, it falls back to
the reactive `401` path, which is always on. `null` (the default) turns it off.

### Persisting tokens

By default tokens live in memory only. Give `auth { }` a `TokenStorage` and
NetFlow seeds from `load()` on the first request and writes back on every change
(refresh, `setTokens`) and when the session ends (`clearTokens`, a failed
refresh):

```kotlin
interface TokenStorage {
    suspend fun load(): BearerTokens?
    suspend fun save(tokens: BearerTokens)
    suspend fun clear()
}

val client = netflowClient {
    baseUrl = "https://api.example.com"
    auth {
        storage(myTokenStorage)                       // load + save + clear, automatic
        refreshTokens { BearerTokens(/* ... */) }     // just return the new tokens
    }
}
```

`loadTokens { }` still works and wins over `storage(...)` for the initial seed.
If a storage call throws, NetFlow swallows it and keeps using the in-memory token.

`netflow-core` ships `InMemoryTokenStorage()` for tests, or for an app that wants
a fresh login every launch. It pulls in no platform storage libraries.

**Persistent storage: `netflow-token-storage`**

```kotlin
implementation("io.github.kmpbits:netflow-token-storage:<latest_version>")
```

`SettingsTokenStorage(settings)` stores the token pair over a
`multiplatform-settings` `Settings`. No encryption of its own, so back it with a
secure store:

```kotlin
// androidMain
val settings = SharedPreferencesSettings(
    EncryptedSharedPreferences.create(
        context, "netflow_auth",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
)

// iosMain
val settings = KeychainSettings(service = "netflow_auth")

// shared
auth {
    storage(SettingsTokenStorage(settings))
    refreshTokens { /* ... */ }
}
```

`androidx.security:security-crypto` is deprecated and has no direct replacement.
If that bothers you, use a plain `SharedPreferencesSettings` (app-private and
sandboxed, but not encrypted at rest) or your own Keystore-backed store. The iOS
Keychain doesn't have this problem.

For DataStore, SQLDelight, or anything else, implement `TokenStorage` yourself.
It's three suspend functions.

The `sample` module has the full wiring: `createTokenStorage()` as an
`expect`/`actual`, Keychain on iOS and `EncryptedSharedPreferences` on Android,
under `sample/.../core/di/`.

Everything here also works through `MockNetFlowClient(auth = { ... })`, so the
whole refresh flow is testable without a network.

---

## Advanced configuration

### Custom headers

```kotlin
client.call {
    path = "/secure-endpoint"
    header(Header(HttpHeader.custom("Authorization"), "Bearer $token"))
}.responseFlow<SecureDataDto, SecureData>(transform = { it.toModel() })
```

### Query parameters

```kotlin
client.call {
    path = "/users"
    parameter("role" to "admin")
    parameter("active" to true)
}.responseFlow<UserDto, User>(transform = { it.toModel() })
```

### Retry

```kotlin
client.call {
    path = "/unstable-endpoint"
    retry {
        times = RetryTimes.THREE
        delay = 1.seconds
        retryOn = { it is IOException }
    }
}.responseFlow<DataDto, Data>(transform = { it.toModel() })
```

### Interceptors

Write it once in `commonMain`, it runs on both engines.

```kotlin
val trace = NetFlowInterceptor { chain ->
    val request = chain.request.newBuilder()
        .header(Header(HttpHeader.custom("X-Trace-Id"), randomTraceId()))
        .build()

    chain.proceed(request)
}

val timing = NetFlowInterceptor { chain ->
    val start = TimeSource.Monotonic.markNow()
    val response = chain.proceed(chain.request)
    log("${chain.request.url} -> ${response.code} in ${start.elapsedNow()}")
    response
}

val client = netflowClient {
    baseUrl = "https://api.example.com"
    addInterceptor(trace)
    addInterceptor(timing)
}
```

Registration order is execution order: `trace` is the outermost.

Not calling `chain.proceed(...)` short-circuits the chain — the network is never touched:

```kotlin
val offline = NetFlowInterceptor { chain ->
    cached(chain.request.url) ?: chain.proceed(chain.request)
}
```

The interceptor runs **inside** the retry loop and **after** auth: it sees the final
`Authorization` header and is called once per attempt.

Since the chain wraps the engine, `MockNetFlowClient` runs it too — your interceptors
are testable without a network call:

```kotlin
val client = MockNetFlowClient(interceptors = listOf(trace)) {
    NetFlowMockResponse.success(body = "[]")
}

client.call { path = "todos" }.response()
assertEquals("abc", client.recordedRequests.single()["X-Trace-Id"])
```

### Certificate pinning

```kotlin
netflowClient {
    baseUrl = "https://api.example.com"
    pinning {
        pin("api.example.com", "sha256/AAAA…=", "sha256/BBBB…=")  // active + backup
        pin("*.example.com", "sha256/CCCC…=")
    }
}
```

Getting a host's SPKI hash:

```bash
openssl s_client -connect api.example.com:443 -servername api.example.com < /dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

> **Always declare a backup pin for the next key.** Without one, certificate rotation
> leaves installed apps unable to connect, with no way to recover short of a new
> release in the store.

On iOS, the supported key types are RSA-2048, RSA-4096, EC P-256 and EC P-384; any
other type is treated as a pin failure. Multiple pins per host are supported, and
`*.host` matches exactly one subdomain level. Hosts with no pin declared are
unaffected, and a pin failure fails the request — there is no report-only mode.

---

## Error handling

`responseToModel` is the only extension that throws — all other extensions return a sealed state.

```kotlin
try {
    val response = client.call {
        path = "/might-fail"
    }.responseToModel<Data>()
} catch (e: NetFlowException) {
    when (e) {
        is NetworkException -> { /* handle network issues */ }
        is SerializationException -> { /* handle parsing errors */ }
        is HttpException -> {
            val code = e.code
            val errorBody = e.errorBody
        }
    }
}
```

---

## Using with DI (e.g. Koin)

```kotlin
single {
    netflowClient {
        baseUrl = "https://api.example.com"
    }
}
```

---

## Roadmap

- Integration tests for the platform HTTP engines (OkHttp / NSURLSession) and coverage reporting (Kover)
- Multipart / form-data support
- WebSocket support — a `client.socket { }` returning a connection-state Flow, in the same house style as `responseFlow`
- Auth: custom `refreshOn { }` predicate (refresh on 403 or an error-body match, not just 401)
- `SettingsTokenStorage` variants for DataStore / SQLDelight

---

## License

This project is licensed under the Apache License, Version 2.0.

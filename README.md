# NetFlow KMP
A lightweight networking library for Kotlin Multiplatform that provides a simple API for Flow and direct suspending calls with optional Jetpack Paging 3 support.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/netflow-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kmpbits/netflow-core)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

## Features

- Kotlin Multiplatform support (Android and iOS)
- Multiple response strategies:
  - Flow (with UI state handling)
  - Async (suspending, one-shot)
  - Paginated (Jetpack Paging 3 via `netflow-paging`)
- Two-type API: separate deserialization type (`ApiType`) from display type (`DisplayType`) — no trailing `.map` needed
- `wrappedResponse` flag for APIs that return `{ "data": ... }` envelopes
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

---

## Getting Started

### Initialize the Client

```kotlin
val client = netFlowClient {
    baseUrl = "https://api.example.com"

    header(Header(HttpHeader.custom("custom-header"), "value"))
    header(Header(HttpHeader.CONTENT_TYPE), "application/json")
}
```

### Basic Request

```kotlin
val response = client.call {
    path = "/users"
    method = HttpMethod.Get
}.response()
```

### Deserialize to Model

```kotlin
val user: User = client.call {
    path = "/users/1"
}.responseToModel<User>()
```

---

## Working with Flow

The two type parameters are `<ApiType, DisplayType>`:
- `ApiType` — the type the JSON is deserialized into (your DTO).
- `DisplayType` — the type emitted to the UI (your domain model).

When both are the same, pass the same type twice.

```kotlin
// Same type — no mapping needed
val flow = client.call {
    path = "/users/1"
}.responseFlow<UserDto, UserDto>()

// Different types — map inside the builder, no .map() at the call site
val flow = client.call {
    path = "/users/1"
}.responseFlow<UserDto, User> {
    apiTransform { it.toModel() }
}
```

### With local cache

```kotlin
val usersFlow = client.call {
    path = "/users"
    method = HttpMethod.Get
}.responseFlow<UserDto, User> {
    apiTransform { it.toModel() }

    onNetworkSuccess { dto ->
        queries.insertUser(dto.toEntity())
    }

    local({ observe { queries.getUser() } }, transform = { it.toDto() })
}
```

The `transform` inside `local()` maps from the database entity type to `ApiType` (the DTO). `apiTransform` then maps from `ApiType` to `DisplayType` before the value is emitted.

### Offline-only

```kotlin
local({
    onlyLocalCall = true
    call { queries.getAllUsers() }
}, transform = { it.toDto() })
```

### Wrapped API responses

For APIs that return `{ "data": { ... } }` instead of a plain object:

```kotlin
responseFlow<UserDto, User> {
    wrappedResponse = true
    apiTransform { it.toModel() }
}
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

```kotlin
suspend fun deleteUser(id: Int): AsyncState<Unit> {
    return client.call {
        path = "users/$id"
        method = HttpMethod.Delete
    }.responseAsync<Unit, Unit> {
        onNetworkSuccess { queries.deleteUser(id) }
    }
}
```

With type mapping:

```kotlin
suspend fun getUser(id: Int): AsyncState<User> {
    return client.call {
        path = "users/$id"
    }.responseAsync<UserDto, User> {
        apiTransform { it.toModel() }
    }
}
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

### Remote + local paging (SQLDelight)

Wrap delete and insert in a **single transaction** so the paging source is invalidated exactly once — after all data is ready.

```kotlin
fun getPosts(): Flow<PagingData<Post>> = client.call {
    path = "/posts"
}.responsePaginated<PostDto, Post> {
    localSource(
        pagingSource = { PostPagingSource(database) },
        transform = { it.toModel() }
    )

    deleteOnRefresh = false // handled inside insertAll
    insertAll(transform = { it.toEntity() }) { posts ->
        database.postQueries.transaction {
            database.postQueries.deleteAll()
            posts.forEach { database.postQueries.insertPost(it) }
        }
    }

    firstItemDatabase(
        itemDatabase = { database.postQueries.getFirstPost().executeAsOneOrNull() },
        timestamp = { it.lastUpdatedTimestamp }
    )
}
```

`pagingSource` is a custom `PagingSource<Int, YourEntity>` backed by your local database. It should use a listener (e.g. SQLDelight's `Query.Listener`) to call `invalidate()` when the underlying data changes, so the UI stays in sync after a network refresh.

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

## Advanced Configuration

### Custom Headers

```kotlin
client.call {
    path = "/secure-endpoint"
    header(Header(HttpHeader.custom("Authorization"), "Bearer $token"))
}.responseFlow<SecureDataDto, SecureData> {
    apiTransform { it.toModel() }
}
```

### Query Parameters

```kotlin
client.call {
    path = "/users"
    parameter("role" to "admin")
    parameter("active" to true)
}.responseFlow<UserDto, User> {
    apiTransform { it.toModel() }
}
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
}.responseFlow<DataDto, Data> { apiTransform { it.toModel() } }
```

---

## Error Handling

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
    netFlowClient {
        baseUrl = "https://api.example.com"
    }
}
```

---

## Roadmap

- Unit tests for `netflow-core` and `netflow-paging` modules
- Multipart / form-data support
- WebSocket support

---

## License

This project is licensed under the Apache License, Version 2.0.

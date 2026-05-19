# NetFlow KMP
A lightweight networking library for Kotlin Multiplatform that provides a simple API for Flow and direct suspending calls — with optional Jetpack Paging 3 support.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/netflow-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kmpbits/netflow-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

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
        queries.insertTodo(dto.toEntity())
    }

    local({ observe { queries.getTodo() } }, transform = { it.toDto() })
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

`responsePaginated` integrates Jetpack Paging 3, supporting both network-only and remote+local (SQLDelight) strategies.

Your API response model must implement `PagingModel`:

```kotlin
@Serializable
data class PostDto(
    val id: Int,
    val title: String,
    override var page: Int = 0,
    override var lastUpdatedTimestamp: Long = 0L
) : PagingModel
```

### Network-only paging

```kotlin
fun getPosts(): Flow<PagingData<Post>> = client.call {
    path = "/posts"
    parameter("page" to 1)
}.responsePaginated<PostDto, Post> {
    onlyApiCall = true
    networkTransform { it.toModel() }
}
```

### Remote + local paging (SQLDelight)

```kotlin
fun getPosts(): Flow<PagingData<Post>> = client.call {
    path = "/posts"
    parameter("page" to 1)
}.responsePaginated<PostDto, Post> {
    localSource(
        pagingSource = {
            QueryPagingSource(
                countQuery = queries.countPosts(),
                transacter = queries,
                context = Dispatchers.IO,
                queryProvider = queries::getPostsPaged
            )
        },
        transform = { it.toModel() }
    )
    insertAll(transform = { it.toEntity() }) { posts ->
        queries.transaction {
            queries.deleteAll()
            posts.forEach { queries.insertPost(it) }
        }
    }
    deleteAll { queries.deleteAll() }
    firstItemDatabase(
        itemDatabase = { queries.getFirstPost().executeAsOneOrNull() },
        timestamp = { it.lastUpdatedTimestamp }
    )
}
```

### PagingBuilder options

| Property | Default | Description |
|---|---|---|
| `defaultPageSize` | `20` | Items loaded per page |
| `pageQueryName` | `"page"` | URL query parameter name for the page number |
| `onlyApiCall` | `false` | `true` for network-only (no local DB) |
| `wrappedResponse` | `false` | `true` when API returns `{ "data": [...] }` |
| `deleteOnRefresh` | `true` | Clear local DB on `REFRESH` load type |
| `refresh` | `false` | Force refresh on start, ignoring cache timeout |
| `cacheTimeout` | `1 hour` | How long before re-fetching from the network |

### Consuming in the ViewModel

```kotlin
val posts = repository.getPosts().cachedIn(viewModelScope)
```

### Consuming in Compose

```kotlin
val posts = viewModel.posts.collectAsLazyPagingItems()

LazyColumn {
    items(count = posts.itemCount, key = posts.itemKey { it.id }) { index ->
        posts[index]?.let { PostItem(it) }
    }
}
```

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

## License

This project is licensed under the MIT License.

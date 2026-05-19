# NetFlow KMP
A lightweight networking library for Kotlin Multiplatform that provides a simple API for Flow, LiveData, and direct suspending calls.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/netflow-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kmpbits/netflow-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Features

- Kotlin Multiplatform support (Android and iOS)
- Multiple response strategies:
  - LiveData
  - Flow (with UI state handling)
  - Direct deserialization
- Customizable requests (headers, parameters, methods)
- Local cache integration with observation support
- Built-in error handling and retry logic
- Debug logging with multiple levels (None, Basic, Headers, Body)

---

## Overview

NetFlow KMP simplifies network requests, caching, and local data synchronization in Kotlin Multiplatform projects.

Key benefits:

- **Automatic synchronization**: Update local databases when API calls succeed to trigger observers automatically.
- **Reduced boilerplate**: Define network success and local observation logic in a single block.
- **Offline-first approach**: Load from local storage while refreshing from the network in the background.
- **Data transformations**: Work with domain models using transformations or use DTOs directly.
- **Multiplatform**: Designed for KMP, supporting Android and iOS.

---

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies { 
    implementation("io.github.kmpbits:netflow-core:<latest_version>")
}
```

Check the latest version on [Maven Central](https://search.maven.org/artifact/com.github.kmpbits.libraries/netflow-core)

---

## Getting Started

### Initialize the Client

```kotlin
val client = netFlowClient {
    baseUrl = "https://api.example.com"

    // Optional default headers
    header(Header(HttpHeader.custom("custom-header"), "This is a custom header"))
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

```kotlin
val userFlow = client.call {
    path = "/users/1"
}.responseFlow<User>()
```

Customize the flow behavior:

```kotlin
val usersFlow = client.call {
  path = "/users"
  method = HttpMethod.Get
}.responseFlow<List<User>> {

  onNetworkSuccess { usersDto ->
    // Convert DTO to Entity table
    userDao.insertAll(users.map(UserDto::toEntity))
  }

  local({
    observe {
      userDao.getAllUsers()
    }
    // Convert Entity to DTO, if the database object is different than the network 
    // because the return type from your database must match the network DTO
  }, transform = { it.map(UserEntity::ToDto) })
}.map {
  // Convert all of the response to models as it is the return type of the function
  it.map { it.map(UserDto::toModel) }
}
```

Important:
- The return type from your database must match the network DTO (e.g., `UserDto`).
- If you're using a different entity class, use the `transform` parameter inside `local()` to convert to the DTO type, otherwise, you will get a `ClassCastException`.
- If you only want to fetch local data without a network call, set `onlyLocalCall = true` inside the `local` DSL block.

```kotlin
local({
    onlyLocalCall = true
    call {
        userDao.getAllUsers()
    }
}, transform = { it.map { dto -> dto.toModel() } })
```

All the responses can be mapped at once using the `map` extension inside `ResultState`:

```kotlin
.map {
    it.map { dtoList -> dtoList.map { it.toModel() } }
}
```

### Observing Flow

Using lifecycle:

```kotlin
userFlow.observe(viewLifecycleOwner) { state ->
    when(state) {
        is ResultState.Loading -> showLoading()
        is ResultState.Success -> showUsers(state.data)
        is ResultState.Error -> showError(state.exception.message)
        is ResultState.Empty -> showEmptyState()
    }
}
```

Or with coroutines:

```kotlin
lifecycleScope.launch {
    userFlow.collectLatest { state ->
        // same logic as above
    }
}
```

---

## Working with Async

NetFlow also supports suspending requests for scenarios where observation is not required.

```kotlin
suspend fun deleteUser(id: Int): AsyncState<User> {
    return client.call {
        path = "users/$id"
        method = HttpMethod.Delete
    }.responseAsync<UserDto> {
        onNetworkSuccess {
            userDao.deleteUser(id)
        }
    }.map(UserDto::toModel)
}
```

---

## Advanced Configuration

### Custom Headers

```kotlin
client.call {
    path = "/secure-endpoint"
    header(Header(HttpHeader.custom("custom-header"), "This is a custom header"))
    header(Header(HttpHeader.CONTENT_TYPE), "application/json")
}.responseFlow<SecureData>()
```

### Query Parameters

```kotlin
client.call {
    path = "/users"
    parameter("role" to "admin")
    parameter("active" to true)
}.responseFlow<List<User>>()
```

---

## Error Handling

`responseToModel` is the only extension that requires a try-catch block.

```kotlin
try {
  val response = client.call {
    path = "/might-fail"
  }.responseToModel<Data>()
} catch (e: StateTalkException) {
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

## Testing

Testing support is under development.

---

## License

This project is licensed under the MIT License.

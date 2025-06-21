# NetFlow KMP 🌐
A lightweight, multiplatform network library for Kotlin – seamless API calls with Flow, LiveData, and native performance.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/netflow-core.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.github.kmpbits.libraries/netflow-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## ✨ Features

- 🚀 Kotlin Multiplatform-ready (Android + iOS)
- 📡 Multiple response strategies:
  - LiveData
  - Flow (with UI state handling)
  - Direct deserialization
- ⚙️ Customizable requests (headers, parameters, methods)
- 🧠 Smart local cache integration with auto-observe
- 🔁 Built-in error handling and retry logic
- 🔍 Debug logging with multiple levels (None, Basic, Headers, Body)

---

## 📦 Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies { 
    implementation("io.github.kmpbits:netflow-core:<latest_version>")
}
```

Check the latest version on [Maven Central](https://search.maven.org/artifact/com.github.kmpbits.libraries/netflow-core)

---

## 🚀 Getting Started

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

## 🌊 Working with Flow

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

    // ✅ Automatically insert into your local database
    onNetworkSuccess { users ->
        userDao.insertAll(users)
    }

    // 👁️ Observe the local data source for UI updates
    local {
        observe {
            userDao.getAllUsers()
        }
    }
}.map {
    // This is an extension function to map the success response to a different model
    it.map { it.map { it.toModel() } }
}
```
⚠️ Important
The return type from your database must match the network DTO (e.g., UserDto).
If you're using a different domain model, it won't work for now.
All the response can be mapped at once with the map extension function inside the ResultState.

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

## 📋 Advanced Configuration

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

## ❗ Error Handling

``responseToModel`` is the only extension that needs to be used with try catch.

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

## 🧰 Using with DI (e.g. Koin)

```kotlin
single {
    netFlowClient {
        baseUrl = "https://api.example.com"
    }
}
```

---

## 🧪 Testing

NetFlow is tested across Android and iOS targets. You can find a working sample app in the `sample/` module.

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
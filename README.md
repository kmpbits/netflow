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

⚠️ **Important**  
The return type from your database must match the network DTO (e.g., UserDto).  
If you're using a different domain model, it won't work for now.  
All the response can be mapped at once with the map extension function inside the ResultState.

---

### 🧩 Local Data Options

Inside the `responseFlow {}` builder, you can attach a local data source integration using the `local {}` DSL. This supports both **live observation** and **one-time retrieval**:

#### ✅ Real-Time Observation with `observe {}`

Watches the local database for changes and emits updates automatically. This requires your DAO to return a `Flow<T>`:

```kotlin
local {
    observe {
        userDao.getAllUsers()
    }
}
```

> ⚠️ **Important Limitation: Local Data Type Must Match DTO**
>
> When using `local.observe` or `local.call` inside `responseFlow<T>()`, the object returned from your local database **must be the same type** as the DTO (`T`) used in the response.
>
> For example, if your `responseFlow<UserDto>()` expects `UserDto`, then your local database must return `Flow<UserDto>` or `UserDto`.  
> You **cannot** return a different entity type (like `UserEntity`) or a domain model (`User`) directly from your local DAO.
>
> This is due to type inference and generic constraints—mismatched types will cause a compilation error.
>
> ### ✅ Planned Improvement
>
> Future versions of NetFlow KMP may support a mapping function, allowing you to convert local data into the correct DTO form like this:
>
> ```kotlin
> local {
>     observe {
>         userDao.getAllUserEntities()
>     }
>     map { entities -> entities.map { it.toDto() } }
> }
> ```
>
> For now, please ensure your local storage layer returns the same type used in the `responseFlow<T>()`.


#### 📦 One-Time Fetch with `call {}`

Fetches a snapshot from your local database only once (non-reactive):

```kotlin
local {
    call {
        userDao.getUserById(1)
    }
}
```

⚠️ **Note**: The return type from your local DB must match the DTO used in the network layer.

```kotlin
.responseFlow<UserDto> {
    local {
        observe { userDao.getUserDto() }
    }
}.map { it.map(UserDto::toModel) }
```

#### ⚡ Default Behavior

When using `observe {}` or `call {}` inside `local {}`, NetFlow will first emit the local database result, and then update it with the network response.  
To only emit the API result and skip local fallback:

```kotlin
local {
    onlyApiCall = true
}
```

---

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

`responseToModel` is the only extension that needs to be used with try-catch.

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
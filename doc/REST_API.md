# REST API Configuration

This package contains the network layer configuration using Ktor Client with Dagger Hilt dependency injection.

## Configuration

### Base URL Configuration

The API base URL is configured via `BuildConfig` and depends on the build type:

**Debug Build:**
- Uses local development server
- Default: `http://192.168.1.100:8080`
- For Android Emulator, use: `http://10.0.2.2:8080` (maps to host's localhost)

**Release Build:**
- Uses production server
- Default: `http://192.168.1.100:8080`
- **TODO:** Update this in `app/build.gradle.kts` before deploying to production

### How to Change URLs

Edit `app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "API_BASE_URL", "\"http://YOUR_LOCAL_IP:PORT\"")
    }
    release {
        buildConfigField("String", "API_BASE_URL", "\"https://api.yourapp.com\"")
    }
}
```

## Features

### HTTP Client Configuration
- **Content Negotiation:** JSON serialization/deserialization with Kotlinx Serialization
- **Timeouts:**
  - Request timeout: 30 seconds
  - Connect timeout: 10 seconds
  - Socket timeout: 30 seconds
- **Logging:** Enabled only in DEBUG builds (INFO level)
- **Cleartext traffic:** Permitted only in debug builds. `network_security_config.xml` and its
  manifest reference live in the `debug` source set / debug manifest overlay, so release APKs
  never declare cleartext HTTP. Point production at an HTTPS URL before shipping.

### Error Handling

The `Result<T>` sealed class provides type-safe error handling:

```kotlin
when (val result = repository.postMessage(message)) {
    is Result.Success -> {
        // Handle successful response
        val data = result.data
    }
    is Result.Error -> {
        // Handle error
        val errorMessage = result.message
        val exception = result.exception
    }
}
```

### Specific Error Types

- **ClientRequestException:** 4xx errors (Bad Request, Not Found, etc.)
- **ServerResponseException:** 5xx errors (Internal Server Error, etc.)
- **SocketTimeoutException:** Request timeouts
- **IOException:** Network connectivity issues
- **Generic Exception:** Unexpected errors

## Usage Example

### 1. Inject Repository

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    fun sendMessage(text: String) {
        viewModelScope.launch {
            when (val result = messageRepository.postMessage(Message(text))) {
                is Result.Success -> {
                    // Show success message
                }
                is Result.Error -> {
                    // Show error message
                    _errorState.value = result.message
                }
            }
        }
    }
}
```

### 2. Add New API Endpoints

Create a new repository interface and implementation following the same pattern as `MessageRepository`.

## Testing

### Testing with Local Server

1. Start your backend server on your local machine
2. Find your local IP address:
   - Windows: `ipconfig`
   - macOS/Linux: `ifconfig` or `ip addr`
3. Update the debug URL in `build.gradle.kts`
4. Build and run the app

### Testing with Android Emulator

If your backend is running on `localhost:8080` on your development machine:
- Use `http://10.0.2.2:8080` as the base URL
- The emulator maps `10.0.2.2` to the host machine's `127.0.0.1`

## Dependencies

```kotlin
implementation("io.ktor:ktor-client-core:2.3.5")
implementation("io.ktor:ktor-client-okhttp:2.3.5")
implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
implementation("io.ktor:ktor-client-logging:2.3.5")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
```

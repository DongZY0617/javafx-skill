# Retrofit Network Integration Reference

> Patterns for integrating `retrofit-spring-boot-starter` into JavaFX + Spring Boot desktop applications. Covers API interface definition, OkHttp client registration, token authentication interceptors, unified response parsing, async execution with UI thread safety, global exception handling, and timeout/retry configuration.
>
> This reference is based on production-proven patterns from the ruoyi-javafx project.

## Why Retrofit for JavaFX

JavaFX desktop apps that communicate with a backend REST API need a clean HTTP layer. `retrofit-spring-boot-starter` (by lianjiatech) provides:

- **Declarative API interfaces** — annotated Java interfaces, no boilerplate HTTP code
- **Spring Boot integration** — auto-scans `@RetrofitClient` interfaces as Spring beans, injectable via `@Resource`
- **Pluggable OkHttpClient** — custom OkHttp instances with interceptors, timeouts, SSL
- **Global retry & logging** — declarative retry and log strategies via YAML config
- **Familiar to backend developers** — same Retrofit patterns used in Android/backend, lower learning curve

## Maven Dependency

```xml
<properties>
    <retrofit.version>4.0.8</retrofit.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.github.lianjiatech</groupId>
        <artifactId>retrofit-spring-boot-starter</artifactId>
        <version>${retrofit.version}</version>
    </dependency>
</dependencies>
```

## Architecture Overview

The recommended layering separates concerns into four layers:

```
Controller (JavaFX UI)
    ↓ calls
Service (business logic + response parsing)
    ↓ calls
Api Interface (Retrofit declarative HTTP)
    ↓ uses
OkHttpClient (interceptors, timeouts, auth)
    ↓ communicates with
Backend REST API
```

| Layer | Responsibility | Package Convention |
|-------|---------------|-------------------|
| API Interface | Declarative HTTP method definitions (`@GET`, `@POST`, etc.) | `retrofit.api` |
| OkHttpClient Config | OkHttp client registration with interceptors and timeouts | `retrofit.config` |
| Interceptors | Cross-cutting concerns (auth token, logging) | `retrofit.interceptor` |
| Context | Shared state (token storage, API constants, response wrapper) | `retrofit.context` |
| Service | Business logic — calls API, parses response, throws on error | `service` |
| Exception | Unified API exception and global handler | `exception` |
| Async | Thread pool for non-blocking UI execution | `util` |

## Configuration (application.yml)

```yaml
app:
  retrofit:
    # Backend base URL — all @RetrofitClient interfaces use this via ${app.retrofit.base-url}
    base-url: http://localhost:8080/
    # OkHttp connect timeout
    connect-timeout: 10s
    # OkHttp read timeout (waiting for response data)
    read-timeout: 10s
    # OkHttp write timeout (sending request data)
    write-timeout: 10s

# Retrofit global config (retrofit-spring-boot-starter native)
retrofit:
  global-log:
    enable: false
    log-level: info
    # NONE / BASIC / HEADERS / BODY (BODY may leak sensitive data — use with caution in prod)
    log-strategy: BASIC
  global-retry:
    enable: false
```

## OkHttpClient Registration

Register a custom OkHttpClient with interceptors and configurable timeouts. The `SourceOkHttpClientRegistrar` SPI lets you name the client and reference it from `@RetrofitClient(sourceOkHttpClient = "...")`.

```java
package com.example.app.retrofit.config;

import com.github.lianjiatech.retrofit.spring.boot.core.SourceOkHttpClientRegistrar;
import com.github.lianjiatech.retrofit.spring.boot.core.SourceOkHttpClientRegistry;
import com.example.app.retrofit.interceptor.TokenInterceptor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RetrofitOkHttpClientRegistrar implements SourceOkHttpClientRegistrar {

    private final TokenInterceptor tokenInterceptor;

    @Value("${app.retrofit.connect-timeout:10s}")
    private Duration connectTimeout;

    @Value("${app.retrofit.read-timeout:10s}")
    private Duration readTimeout;

    @Value("${app.retrofit.write-timeout:10s}")
    private Duration writeTimeout;

    public RetrofitOkHttpClientRegistrar(TokenInterceptor tokenInterceptor) {
        this.tokenInterceptor = tokenInterceptor;
    }

    @Override
    public void register(SourceOkHttpClientRegistry registry) {
        registry.register("appOkHttpClient", new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .writeTimeout(writeTimeout)
                .addInterceptor(tokenInterceptor)
                .build());
    }
}
```

## API Interface Definition

Define HTTP APIs as annotated Java interfaces. Each interface is annotated with `@RetrofitClient` specifying the base URL and OkHttpClient. Methods return `Map<String, Object>` (parsed by the Service layer into typed objects).

```java
package com.example.app.retrofit.api;

import com.github.lianjiatech.retrofit.spring.boot.core.RetrofitClient;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

@RetrofitClient(baseUrl = "${app.retrofit.base-url}", sourceOkHttpClient = "appOkHttpClient")
public interface SysUserApi {

    @GET("system/user/list")
    Map<String, Object> list(@QueryMap Map<String, Object> queryParams);

    @GET("system/user/{userId}")
    Map<String, Object> getById(@Path("userId") Long userId);

    @POST("system/user")
    Map<String, Object> add(@Body Map<String, Object> user);

    @PUT("system/user")
    Map<String, Object> edit(@Body Map<String, Object> user);

    @DELETE("system/user/{userIds}")
    Map<String, Object> remove(@Path("userIds") String userIds);
}
```

### Annotation Quick Reference

| Annotation | HTTP Method | Typical Use |
|-----------|-------------|-------------|
| `@GET("path")` | GET | Query resources, list with `@QueryMap` |
| `@POST("path")` | POST | Create resource with `@Body` |
| `@PUT("path")` | PUT | Update resource with `@Body` |
| `@DELETE("path")` | DELETE | Remove resource with `@Path` |
| `@QueryMap` | — | Map of query parameters (pagination, filters) |
| `@Path("name")` | — | URL path substitution (`/user/{userId}`) |
| `@Body` | — | Request body (serialized to JSON) |

## Token Authentication

### Token Storage

Store the auth token in a static context with `volatile` visibility and `char[]` for reduced memory footprint. The `clear()` method actively zeroes the array to minimize token residency in memory.

```java
package com.example.app.retrofit.context;

public final class TokenContext {

    private static volatile char[] tokenChars;

    private TokenContext() {}

    public static String getToken() {
        char[] chars = tokenChars;
        return chars != null ? new String(chars) : null;
    }

    public static void setToken(String token) {
        clear();
        tokenChars = (token != null) ? token.toCharArray() : null;
    }

    public static void clear() {
        char[] chars = tokenChars;
        if (chars != null) {
            java.util.Arrays.fill(chars, '\0');
        }
        tokenChars = null;
    }
}
```

### Token Interceptor

An OkHttp interceptor that automatically attaches the `Authorization: Bearer <token>` header to every request if a token is available.

```java
package com.example.app.retrofit.interceptor;

import com.example.app.retrofit.context.TokenContext;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenInterceptor implements Interceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = TokenContext.getToken();
        if (token != null && !token.isEmpty()) {
            Request request = original.newBuilder()
                    .header(AUTH_HEADER, BEARER_PREFIX + token)
                    .build();
            return chain.proceed(request);
        }
        return chain.proceed(original);
    }
}
```

## Unified Response Parsing (ApiResult)

The backend returns a unified response envelope `{code, msg, data/rows, total}`. The `ApiResult` wrapper centralizes response parsing, success assertion, and typed data extraction.

```java
package com.example.app.retrofit.context;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.app.exception.ApiException;
import lombok.Getter;

import java.util.Map;

@Getter
public class ApiResult {

    private final int code;
    private final String msg;
    private final Map<String, Object> raw;

    private ApiResult(int code, String msg, Map<String, Object> raw) {
        this.code = code;
        this.msg = msg;
        this.raw = raw;
    }

    public static ApiResult of(Map<String, Object> raw) {
        if (raw == null) {
            throw new ApiException("Server returned null");
        }
        Object codeObj = raw.get("code");
        int code = (codeObj instanceof Number) ? ((Number) codeObj).intValue() : -1;
        String msg = String.valueOf(raw.getOrDefault("msg", ""));
        return new ApiResult(code, msg, raw);
    }

    /**
     * Assert success. If unauthorized (401), fire auth-expired callback before throwing.
     * Chainable — returns this on success.
     */
    public ApiResult assertSuccess() {
        if (AuthContextHolder.isUnauthorized(code)) {
            AuthContextHolder.fireAuthExpired();
            throw new ApiException(code, msg);
        }
        if (code != ApiConstants.CODE_SUCCESS) {
            throw new ApiException(code, msg);
        }
        return this;
    }

    public Integer getInt(String key) {
        Object val = raw.get(key);
        return (val instanceof Number) ? ((Number) val).intValue() : null;
    }

    public String getString(String key) {
        Object val = raw.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    public <T> T getDataAs(Class<T> clazz) {
        Object data = raw.get("data");
        if (data == null) return null;
        if (clazz.isInstance(data)) return clazz.cast(data);
        return JSON.to(clazz, data);
    }

    public JSONObject getDataAsJson() {
        Object data = raw.get("data");
        if (data == null) return new JSONObject();
        if (data instanceof Map) {
            return new JSONObject((Map<String, Object>) data);
        }
        return JSONObject.from(data);
    }

    public JSONArray getDataArray() {
        Object data = raw.get("data");
        if (data == null) data = raw.get("rows");
        if (data == null) return new JSONArray();
        if (data instanceof java.util.List) return new JSONArray((java.util.List<?>) data);
        return JSONArray.from(data);
    }
}
```

### API Constants

```java
package com.example.app.retrofit.context;

public final class ApiConstants {
    public static final int CODE_SUCCESS = 200;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_SERVER_ERROR = 500;

    private ApiConstants() {
        throw new UnsupportedOperationException("Constants class not instantiable");
    }
}
```

## Auth Expiry Handling

When a 401 response is received, the app must navigate the user back to the login screen. `AuthContextHolder` manages auth-expired listeners with CAS-guarded navigation to prevent duplicate dialogs.

```java
package com.example.app.retrofit.context;

import com.example.app.exception.ApiException;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AuthContextHolder {

    private static final List<Runnable> authExpiredListeners = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean navigating = new AtomicBoolean(false);

    private AuthContextHolder() {}

    public static void addAuthExpiredListener(Runnable listener) {
        if (listener != null) authExpiredListeners.add(listener);
    }

    public static void fireAuthExpired() {
        if (!authExpiredListeners.isEmpty() && navigating.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                try {
                    for (Runnable listener : authExpiredListeners) {
                        try {
                            listener.run();
                        } catch (Exception e) {
                            Thread.currentThread().getUncaughtExceptionHandler()
                                    .uncaughtException(Thread.currentThread(), e);
                        }
                    }
                } finally {
                    navigating.set(false);
                }
            });
        }
    }

    public static boolean isUnauthorized(int code) {
        return code == ApiConstants.CODE_UNAUTHORIZED;
    }

    /** Recursively check if an exception chain contains a 401 ApiException */
    public static boolean isAuthExpired(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof ApiException apiEx && isUnauthorized(apiEx.getCode())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
```

## Service Layer

The Service layer wraps API calls with `ApiResult.of(...).assertSuccess()` — this is the core pattern. The Service handles response parsing, typed conversion, and business-specific parameter assembly, so Controllers stay focused on UI.

```java
package com.example.app.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.app.model.UserVO;
import com.example.app.retrofit.api.SysUserApi;
import com.example.app.retrofit.context.ApiResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class UserService {

    @Resource
    private SysUserApi sysUserApi;

    /**
     * Paginated user list (strongly typed).
     * Core pattern: ApiResult.of(api.call(params)).assertSuccess() → extract data
     */
    public PageResult<UserVO> listVOs(Map<String, Object> params) {
        ApiResult result = ApiResult.of(sysUserApi.list(params)).assertSuccess();
        int total = result.getInt("total") != null ? result.getInt("total") : 0;
        JSONArray rows = result.getRows();
        List<UserVO> voList = rows != null ? rows.toJavaList(JSONObject.class).stream()
                .map(UserVO::from)
                .toList() : List.of();
        return PageResult.of(voList, total);
    }

    public void add(Map<String, Object> user) {
        ApiResult.of(sysUserApi.add(user)).assertSuccess();
    }

    public void edit(Map<String, Object> user) {
        ApiResult.of(sysUserApi.edit(user)).assertSuccess();
    }

    public void remove(String userIds) {
        ApiResult.of(sysUserApi.remove(userIds)).assertSuccess();
    }

    public void resetPwd(Long userId, String password) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", userId);
        params.put("password", password);
        ApiResult.of(sysUserApi.resetPwd(params)).assertSuccess();
    }
}
```

### Service Pattern Summary

Every Service method follows the same shape:

```java
public ReturnType doSomething(params) {
    // 1. Call API → get raw Map
    // 2. Wrap with ApiResult.of() → parse code/msg
    // 3. assertSuccess() → throw ApiException if code != 200 (or fire auth-expired if 401)
    // 4. Extract typed data from result
    // 5. Return typed object
}
```

## Async Execution & UI Thread Safety

**Critical**: Retrofit calls are synchronous and blocking — they MUST NOT run on the JavaFX Application Thread. Use a dedicated thread pool and switch back to the UI thread for rendering.

### AsyncUtil

A fixed thread pool with daemon threads, custom rejection policy (discard oldest, never block UI thread), and `runLater()` helper for safe UI thread switching.

```java
package com.example.app.util;

import com.example.app.retrofit.context.AuthContextHolder;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public final class AsyncUtil {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 16;
    private static final long KEEP_ALIVE_SECONDS = 60L;
    private static final int QUEUE_CAPACITY = 256;

    private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            r -> {
                Thread t = new Thread(r, "fx-async-" + new AtomicInteger().incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy() {
                @Override
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    log.warn("Async queue full, discarding oldest task");
                    super.rejectedExecution(r, executor);
                }
            }
    );

    private AsyncUtil() {}

    /** Run task in background. Auth-expired exceptions are silently ignored. */
    public static void run(Runnable task) {
        EXECUTOR.submit(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                if (AuthContextHolder.isAuthExpired(e)) return;
                log.error("Async task failed", e);
                runLater(() -> AlertUtil.error("Async error: " + e.getMessage()));
            }
        });
    }

    /** Run task in background with error callback on UI thread. */
    public static void run(Runnable task, Runnable onError) {
        EXECUTOR.submit(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                if (AuthContextHolder.isAuthExpired(e)) return;
                log.error("Async task failed", e);
                runLater(onError);
            }
        });
    }

    /** Switch to JavaFX Application Thread. Runs immediately if already on UI thread. */
    public static void runLater(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    public static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### Controller Calling Pattern

Controllers call Services asynchronously, then update UI on the JavaFX Application Thread:

```java
// In a Controller event handler
AsyncUtil.run(() -> {
    // Background thread — blocking API call is safe here
    PageResult<UserVO> result = userService.listVOs(params);

    // Switch to UI thread for rendering
    AsyncUtil.runLater(() -> {
        tableView.getItems().setAll(result.getList());
        pagination.setTotalCount(result.getTotal());
    });
});
```

## Exception Handling

### ApiException

Carries the backend response code and message:

```java
package com.example.app.exception;

import com.example.app.retrofit.context.ApiConstants;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ApiException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ApiException(String message) {
        this(ApiConstants.CODE_SERVER_ERROR, message);
    }

    public ApiException(String message, Throwable cause) {
        this(ApiConstants.CODE_SERVER_ERROR, message, cause);
    }
}
```

### GlobalExceptionHandler

Installed as the thread uncaught exception handler. Features:
- Auth-expired exceptions are silently ignored (handled by `AuthContextHolder.fireAuthExpired()`)
- Dialog throttling: same message within 2 seconds shows only once (prevents dialog spam on batch failures)
- Always switches to UI thread via `Platform.runLater` for dialog display

```java
package com.example.app.exception;

import com.example.app.retrofit.context.AuthContextHolder;
import com.example.app.util.AlertUtil;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final long THROTTLE_MS = 2000L;
    private String lastMessage = null;
    private long lastShownTime = 0;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Uncaught exception [{}]: {}", t.getName(), e.getMessage(), e);
        if (AuthContextHolder.isAuthExpired(e)) return;

        String message = extractMessage(e);
        if (shouldThrottle(message)) {
            log.debug("Dialog throttled: '{}'", message);
            return;
        }
        Platform.runLater(() -> AlertUtil.error(message));
    }

    private synchronized boolean shouldThrottle(String message) {
        long now = System.currentTimeMillis();
        if (message.equals(lastMessage) && (now - lastShownTime) < THROTTLE_MS) {
            return true;
        }
        lastMessage = message;
        lastShownTime = now;
        return false;
    }

    private String extractMessage(Throwable e) {
        String message = null;
        if (e instanceof ApiException) {
            message = e.getMessage();
        } else if (e.getCause() != null) {
            message = e.getCause().getMessage();
        }
        if (message == null) message = e.getMessage();
        return message != null ? message : "Unknown error";
    }
}
```

## Password Encryption (RSA)

For login/register flows, encrypt passwords with RSA before transmission. The public key is fetched from the backend:

```java
public String login(LoginRequest request) {
    // 1. Fetch RSA public key from backend
    String publicKey = getPublicKey();
    // 2. Encrypt password
    String encryptedPassword = RsaEncryptor.encrypt(request.getPassword(), publicKey);
    request.setPassword(encryptedPassword);
    // 3. Call login API
    ApiResult result = ApiResult.of(sysLoginApi.login(request)).assertSuccess();
    String token = result.getString("token");
    if (token == null || token.isEmpty()) {
        throw new ApiException("Login returned empty token");
    }
    return token;
}
```

## Complete Integration Checklist

When adding Retrofit to a new JavaFX + Spring Boot project:

1. [ ] Add `retrofit-spring-boot-starter` dependency to `pom.xml`
2. [ ] Configure `app.retrofit.*` and `retrofit.*` in `application.yml`
3. [ ] Create `retrofit/config/RetrofitOkHttpClientRegistrar.java` — register named OkHttpClient
4. [ ] Create `retrofit/interceptor/TokenInterceptor.java` — auth header injection
5. [ ] Create `retrofit/context/TokenContext.java` — volatile char[] token storage
6. [ ] Create `retrofit/context/ApiConstants.java` — response code constants
7. [ ] Create `retrofit/context/ApiResult.java` — unified response wrapper with assertSuccess()
8. [ ] Create `retrofit/context/AuthContextHolder.java` — auth-expired listener management
9. [ ] Create `exception/ApiException.java` — API exception with code
10. [ ] Create `exception/GlobalExceptionHandler.java` — uncaught handler with throttling
11. [ ] Create `util/AsyncUtil.java` — thread pool + runLater helper
12. [ ] Create `retrofit/api/*Api.java` — one interface per backend module
13. [ ] Create `service/*Service.java` — one service per API, wraps with ApiResult.of().assertSuccess()
14. [ ] Register `GlobalExceptionHandler` as thread default uncaught exception handler in app startup
15. [ ] Register auth-expired listener via `AuthContextHolder.addAuthExpiredListener()` to navigate to login

## Common Pitfalls

1. **Blocking the UI thread**: Never call API methods directly in a JavaFX event handler — always wrap with `AsyncUtil.run()`. Retrofit calls are synchronous
2. **Forgetting assertSuccess()**: Calling `ApiResult.of(api.call())` without `assertSuccess()` silently ignores errors — always chain `.assertSuccess()`
3. **Auth-expired dialog spam**: Without `AuthContextHolder`'s CAS guard, multiple concurrent 401 responses can trigger multiple login navigations
4. **CallerRunsPolicy rejection**: Using `CallerRunsPolicy` in the thread pool would run rejected tasks on the UI thread — use `DiscardOldestPolicy` instead
5. **Token in String pool**: Using `String` for token storage keeps it in the JVM string pool longer — use `char[]` with active `clear()` zeroing
6. **No dialog throttling**: Batch operations failing simultaneously can spawn dozens of error dialogs — throttle by message within a time window

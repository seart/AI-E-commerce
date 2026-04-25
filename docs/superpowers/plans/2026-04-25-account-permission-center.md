# Account Permission Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the custom MVC authentication interceptors with Spring Security + JWT + lightweight RBAC while keeping the existing API response contract stable.

**Architecture:** Spring Security owns request authentication, authorization, CORS, and 401/403 responses. Business services continue to use the existing DTOs and `UserContext`, but `UserContext` reads the authenticated principal from `SecurityContextHolder`. JWT remains HS256 and Redis continues to hold runtime blacklist and rate-limit state.

**Tech Stack:** Java 17, Spring Boot 3.3.5, Spring Security, Maven, MyBatis-Plus, Redis, MySQL.

---

## File Structure

- Modify `backend/pom.xml`: add `spring-boot-starter-security`.
- Modify `backend/src/main/java/com/jingdong/backend/config/WebConfig.java`: remove MVC interceptor registration and keep only non-security MVC configuration if still needed.
- Create `backend/src/main/java/com/jingdong/backend/config/SecurityConfig.java`: define stateless security chain, CORS, public paths, admin RBAC, and JWT filter.
- Create `backend/src/main/java/com/jingdong/backend/auth/CurrentUserPrincipal.java`: typed authenticated principal.
- Create `backend/src/main/java/com/jingdong/backend/auth/JwtAuthenticationFilter.java`: validate access tokens and populate `SecurityContextHolder`.
- Create `backend/src/main/java/com/jingdong/backend/auth/SecurityErrorHandler.java`: write existing `ApiResponse` JSON for 401 and 403.
- Modify `backend/src/main/java/com/jingdong/backend/auth/JwtTokenService.java`: expose parsed claims, token type, role, jti, and validation helpers.
- Modify `backend/src/main/java/com/jingdong/backend/auth/TokenBlacklistService.java`: support blacklisting by raw token or `jti`.
- Modify `backend/src/main/java/com/jingdong/backend/auth/UserContext.java`: read current user from Spring Security.
- Modify `backend/src/main/java/com/jingdong/backend/service/AuthService.java`: issue role-aware tokens, add refresh flow, keep logout blacklist behavior.
- Modify `backend/src/main/java/com/jingdong/backend/dto/auth/AuthDtos.java`: add refresh request DTO if missing.
- Modify `backend/src/main/java/com/jingdong/backend/controller/AuthController.java`: add `POST /auth/refresh`.
- Delete `backend/src/main/java/com/jingdong/backend/auth/AuthInterceptor.java` and `backend/src/main/java/com/jingdong/backend/auth/AdminInterceptor.java` after replacement is working.
- Modify `backend/src/test/java/com/jingdong/backend/AuthLogoutIntegrationTests.java`: keep logout tests and add refresh/security tests.
- Modify `backend/src/test/java/com/jingdong/backend/ThirdStageIntegrationTests.java`: keep admin/customer 403 assertions compatible with Spring Security.

## Task 1: Add Spring Security Dependency And Red Tests

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/test/java/com/jingdong/backend/AuthLogoutIntegrationTests.java`

- [ ] **Step 1: Add Spring Security dependency**

Add this dependency after `spring-boot-starter-validation`:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

- [ ] **Step 2: Add tests for refresh and token-type enforcement**

Extend `AuthLogoutIntegrationTests` with:

```java
@Test
void refreshTokenIssuesNewAccessToken() {
  ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
      "/auth/login",
      Map.of("mobile", "13800000000", "password", "123456"),
      Map.class
  );
  assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

  Map<String, Object> loginData = data(loginResponse);
  String refreshToken = (String) loginData.get("refreshToken");

  ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
      "/auth/refresh",
      Map.of("refreshToken", refreshToken),
      Map.class
  );
  assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

  String accessToken = (String) data(refreshResponse).get("accessToken");
  HttpHeaders headers = new HttpHeaders();
  headers.setBearerAuth(accessToken);
  ResponseEntity<Map> profileResponse = restTemplate.exchange(
      "/profile",
      HttpMethod.GET,
      new HttpEntity<>(headers),
      Map.class
  );
  assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
}

@Test
void refreshTokenCannotAccessBusinessApis() {
  ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
      "/auth/login",
      Map.of("mobile", "13800000000", "password", "123456"),
      Map.class
  );
  assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

  String refreshToken = (String) data(loginResponse).get("refreshToken");
  HttpHeaders headers = new HttpHeaders();
  headers.setBearerAuth(refreshToken);
  ResponseEntity<Map> profileResponse = restTemplate.exchange(
      "/profile",
      HttpMethod.GET,
      new HttpEntity<>(headers),
      Map.class
  );
  assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}
```

- [ ] **Step 3: Run targeted tests to confirm current red state**

Run:

```bash
cd backend && mvn -Dtest=AuthLogoutIntegrationTests test
```

Expected before implementation: compilation or test failure because `/auth/refresh` does not exist and Spring Security is not configured.

## Task 2: Implement Security Principal And JWT Claims

**Files:**
- Create: `backend/src/main/java/com/jingdong/backend/auth/CurrentUserPrincipal.java`
- Modify: `backend/src/main/java/com/jingdong/backend/auth/JwtTokenService.java`

- [ ] **Step 1: Create `CurrentUserPrincipal`**

```java
package com.jingdong.backend.auth;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record CurrentUserPrincipal(
    String userId,
    String mobile,
    String role,
    String status
) {
  public Collection<? extends GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }
}
```

- [ ] **Step 2: Extend JWT claims**

Change `JwtTokenService` so access and refresh token creation accepts both `userId` and `role`, emits `role`, `typ`, `jti`, and exposes:

```java
public String createAccessToken(String userId, String role)
public String createRefreshToken(String userId, String role)
public TokenClaims parseAccessToken(String token)
public TokenClaims parseRefreshToken(String token)
public TokenClaims parseClaims(String token)
public record TokenClaims(String userId, String role, String type, String jti, Instant expiresAt) {}
```

Keep `createAccessToken(String userId)` and `createRefreshToken(String userId)` temporarily if needed by callers, but update production callers to use the role-aware versions.

- [ ] **Step 3: Run compile**

Run:

```bash
cd backend && mvn -DskipTests compile
```

Expected: compile succeeds after callers are updated in later tasks; it may fail in this task until `AuthService` is updated.

## Task 3: Add Spring Security Chain And Error Handlers

**Files:**
- Create: `backend/src/main/java/com/jingdong/backend/auth/SecurityErrorHandler.java`
- Create: `backend/src/main/java/com/jingdong/backend/auth/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/jingdong/backend/config/SecurityConfig.java`
- Modify: `backend/src/main/java/com/jingdong/backend/config/WebConfig.java`

- [ ] **Step 1: Create `SecurityErrorHandler`**

It must write `ApiResponse.failure(...)` using `ErrorCode.UNAUTHORIZED` for authentication failures and `ErrorCode.FORBIDDEN` for access denied.

- [ ] **Step 2: Create `JwtAuthenticationFilter`**

The filter must:

- skip public paths
- parse `Authorization: Bearer <token>`
- reject blacklisted tokens
- require `typ=access`
- load user from `DatabaseStore`
- reject missing or disabled users
- create `UsernamePasswordAuthenticationToken` with `CurrentUserPrincipal`

- [ ] **Step 3: Create `SecurityConfig`**

Configure:

```java
session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
csrf.disable()
formLogin.disable()
httpBasic.disable()
authorizeHttpRequests:
  public paths -> permitAll()
  /admin/** -> hasAnyRole("ADMIN", "OPERATOR")
  everything else -> authenticated()
addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

- [ ] **Step 4: Remove MVC interceptor registration**

`WebConfig` should no longer inject or register `AuthInterceptor` and `AdminInterceptor`.

## Task 4: Update Auth Service, UserContext, And Controller

**Files:**
- Modify: `backend/src/main/java/com/jingdong/backend/auth/UserContext.java`
- Modify: `backend/src/main/java/com/jingdong/backend/service/AuthService.java`
- Modify: `backend/src/main/java/com/jingdong/backend/dto/auth/AuthDtos.java`
- Modify: `backend/src/main/java/com/jingdong/backend/controller/AuthController.java`

- [ ] **Step 1: Change `UserContext`**

Read `CurrentUserPrincipal` from `SecurityContextHolder.getContext().getAuthentication()`.

- [ ] **Step 2: Add refresh DTO**

Add:

```java
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
```

- [ ] **Step 3: Update `AuthService.session`**

Issue access and refresh tokens with `user.id()` and `user.role()`.

- [ ] **Step 4: Add `AuthService.refresh`**

Parse refresh token, check blacklist and user status, then return a new `UserSessionResponse`.

- [ ] **Step 5: Add controller endpoint**

Add:

```java
@PostMapping("/refresh")
public ApiResponse<UserSessionResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
  return ApiResponse.success(authService.refresh(request));
}
```

## Task 5: Remove Old Interceptors And Update Tests

**Files:**
- Delete: `backend/src/main/java/com/jingdong/backend/auth/AuthInterceptor.java`
- Delete: `backend/src/main/java/com/jingdong/backend/auth/AdminInterceptor.java`
- Modify: `backend/src/test/java/com/jingdong/backend/AuthLogoutIntegrationTests.java`
- Modify: `backend/src/test/java/com/jingdong/backend/ThirdStageIntegrationTests.java`

- [ ] **Step 1: Delete old interceptors**

Remove both files after Spring Security owns the behavior.

- [ ] **Step 2: Ensure tests assert Spring Security behavior**

Keep existing assertions:

- logout blacklists current access token
- login after logout issues fresh usable token
- customer receives `403` for `/admin/dashboard/summary`
- admin receives `200`
- legacy password upgrades to PBKDF2

Add assertions from Task 1 for refresh token behavior.

## Task 6: Verification

**Files:**
- Verify full backend behavior.

- [ ] **Step 1: Compile without tests**

Run:

```bash
cd backend && mvn -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run full backend tests**

Run:

```bash
cd backend && mvn clean test
```

Expected: all tests pass. In the Codex sandbox, this may require approval because tests connect to local MySQL/Redis. If RocketMQ client logging is active, run Maven with:

```bash
cd backend && mvn -Drocketmq.log.root=logs/rocketmqlogs clean test
```

The project root `.gitignore` already ignores `logs/`.

- [ ] **Step 3: HTTP smoke**

Start backend and verify:

- `POST /api/auth/login` as customer succeeds
- `GET /api/profile` with customer token succeeds
- `GET /api/admin/dashboard/summary` with customer token returns `403`
- `POST /api/auth/logout` blacklists token
- `POST /api/auth/refresh` with refresh token returns new session

## Self-Review

- Spec coverage: Spring Security, JWT, RBAC, refresh token, logout blacklist, admin 403, disabled user behavior, and audit compatibility are represented.
- No placeholder scan issues: this plan contains concrete files, commands, and expected behavior.
- Type consistency: `CurrentUserPrincipal`, `TokenClaims`, `RefreshTokenRequest`, and `UserSessionResponse` names are used consistently.

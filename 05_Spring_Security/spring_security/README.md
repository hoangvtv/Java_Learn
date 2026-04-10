# Spring Security

## Mục lục

1. [Sơ đồ luồng xác thực](#sơ-đồ-luồng-xác-thực-request-trong-spring-security)
2. [Giải thích chi tiết từng thành phần](#giải-thích-chi-tiết-từng-thành-phần)
   - [1. User Request → Security Filter Chain](#1-user-request--security-filter-chain)
     - [Bảng 16 filter và thứ tự](#sơ-đồ-luồng-request-qua-16-filter)
     - [3 filter quan trọng nhất](#chi-tiết-3-filter-quan-trọng-nhất)
     - [Thứ tự filter có thể thay đổi không?](#thứ-tự-filter-có-thể-thay-đổi-không)
   - [2. SecurityContextHolder](#2-securitycontextholder)
   - [3. Authentication Manager](#3-authentication-manager)
   - [4. Authentication Provider (1, 2, 3)](#4-authentication-provider-1-2-3)
   - [5. Password Encoder](#5-password-encoder)
   - [6. User Details Service](#6-user-details-service)
   - [7. Docker (DBS)](#7-docker-dbs)
3. [Luồng xác thực tổng hợp](#luồng-xác-thực-tổng-hợp)
4. [Code mẫu SecurityConfig](#code-mẫu-securityconfig)
5. [Các phương thức xác thực](#các-phương-thức-xác-thực-authentication-methods)
   - [1. Basic Authentication](#1-basic-authentication)
   - [2. Digest Authentication](#2-digest-authentication)
   - [3. SSL Client Authentication](#3-ssl-client-authentication-x509-certificate)
   - [4. Form-based Authentication](#4-form-based-authentication)
   - [So sánh 4 phương thức](#so-sánh-4-phương-thức-xác-thực)
6. [Các cách Custom User](#các-cách-custom-user-trong-spring-security)
   - [C1: Cấu hình trong SecurityConfig](#c1-cấu-hình-trong-securityconfig)
   - [C2: Custom UserDetailsService](#c2-custom-userdetailsservice)
   - [So sánh C1 vs C2](#so-sánh-c1-vs-c2)
   - [Bonus: Mã hóa password](#bonus-mã-hóa-password-khi-lưu-vào-database)
7. [Các mô hình phân quyền](#các-mô-hình-phân-quyền-authorization-models)
   - [1. RBAC — Role-Based Access Control](#1-rbac--role-based-access-control-phân-quyền-theo-vai-trò)
   - [2. ABAC — Attribute-Based Access Control](#2-abac--attribute-based-access-control-phân-quyền-theo-thuộc-tính)
   - [3. DAC — Discretionary Access Control](#3-dac--discretionary-access-control-phân-quyền-tự-do)
   - [4. MAC — Mandatory Access Control](#4-mac--mandatory-access-control-phân-quyền-bắt-buộc)
   - [So sánh 4 mô hình](#so-sánh-4-mô-hình-phân-quyền)
   - [Kết hợp RBAC + ABAC](#kết-hợp-rbac--abac-trong-spring-security-thực-tế-nhất)
8. [Bảng tổng hợp màu sắc sơ đồ](#bảng-tổng-hợp-màu-sắc-sơ-đồ)

---

## Sơ đồ luồng xác thực request trong Spring Security

![Spring Security Architecture](src/main/resources/static/img/spring_security.png)

```
┌──────────────────┐
│   USER REQUEST   │  (green)
└────────┬─────────┘
         │
         ▼
┌───────────────────────────────────┐
│      SECURITY FILTER CHAIN        │  (black / orange header)
│  ┌─────────────────────────────┐  │
│  │   Security Filter 1         │  │
│  ├─────────────────────────────┤  │
│  │   Security Filter 2         │  │
│  ├─────────────────────────────┤  │
│  │   Security Filter 3         │  │
│  ├─────────────────────────────┤  │
│  │            ...              │  │
│  ├─────────────────────────────┤  │
│  │   Security Filter N         │  │
│  └─────────────────────────────┘  │
└────────┬────────────┬──────────────┘
         │            │
         ▼            ▼
┌────────────────┐    ┌─────────────────────────────────┐
│   USER         │    │      SECURITY CONTEXT HOLDER    │  (red/pink)
│   RESPONSE     │    │  ┌────────────┐ ┌────────────┐  │
│   (green)      │    │  │ Authorities│ │ Principal │  │
└────────────────┘    │  └────────────┘ └────────────┘  │
                      └─────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│         AUTHENTICATION MANAGER            │  (yellow)
└────────┬─────────┬──────────┬───────────┘
         │         │          │
         ▼         ▼          ▼
┌───────────┐ ┌───────────┐ ┌───────────┐
│  Provider │ │  Provider │ │  Provider │
│     1     │ │     2     │ │     3     │  (pink)
└───────────┘ └─────┬─────┘ └───────────┘
                    │
        ┌───────────┴───────────┐
        │                       │
        ▼                       ▼
┌─────────────────┐  ┌──────────────────────┐
│ Password Encoder │  │  User Details Svc   │  (light blue)
└─────────────────┘  └──────────┬───────────┘
                                 │
                                 ▼
                          ┌─────────────┐
                          │ Docker (DBS)│  (light blue)
                          └─────────────┘
```

## Giải thích chi tiết từng thành phần

### 1. User Request → Security Filter Chain

```
User Request  ──▶  Security Filter Chain
                        │
                        ├── Security Filter 1  (kiểm tra đầu tiên)
                        ├── Security Filter 2
                        ├── Security Filter 3
                        ├── ...
                        └── Security Filter N
```

- **User Request**: Yêu cầu từ phía người dùng gửi lên server (chưa xác thực).
- **Security Filter Chain**: Chuỗi 16 bộ lọc bảo mật được thực thi **theo thứ tự từ trên xuống dưới**. Mỗi filter đảm nhận một nhiệm vụ riêng.

Dưới đây là thứ tự và chức năng của **tất cả 16 filter** trong Security Filter Chain:

| # | Filter | Thứ tự | Chức năng |
|---|---|---|---|
| 0 | `DisableEncodeUrlFilter` | 1 | Vô hiệu hóa mã hóa URL không cần thiết. Tối ưu URL cho Spring. |
| 1 | `WebAsyncManagerIntegrationFilter` | 2 | Tích hợp `SecurityContext` vào `WebAsyncManager` để `SecurityContext` hoạt động trong thread async. |
| 2 | `SecurityContextHolderFilter` | 3 | **Lưu / khôi phục** `SecurityContext` cho mỗi request. Đọc `SecurityContext` từ session hoặc tạo mới rỗng. |
| 3 | `HeaderWriterFilter` | 4 | Ghi các header bảo mật vào response: `X-Frame-Options`, `X-Content-Type-Options`, `Cache-Control`... |
| 4 | `CsrfFilter` | 5 | Bảo vệ chống **CSRF (Cross-Site Request Forgery)**. Yêu cầu request phải có đúng CSRF token. |
| 5 | `LogoutFilter` | 6 | Xử lý **logout**. Bắt URL `/logout` (mặc định) → xóa session, xóa `SecurityContext`, chuyển hướng. |
| 6 | `UsernamePasswordAuthenticationFilter` | 7 | Thu thập thông tin đăng nhập (username/password) → gọi `AuthenticationManager` để xác thực. |
| 7 | `DefaultResourcesFilter` | 8 | Cho phép truy cập tĩnh (CSS, JS, hình ảnh...) mà **không cần** xác thực. |
| 8 | `DefaultLoginPageGeneratingFilter` | 9 | Tự động sinh **trang login** mặc định của Spring Security (khi dùng `formLogin()`). |
| 9 | `DefaultLogoutPageGeneratingFilter` | 10 | Tự động sinh **trang logout** mặc định. |
| 10 | `BasicAuthenticationFilter` | 11 | Xử lý **Basic Authentication** — đọc header `Authorization: Basic base64(user:pass)` → xác thực. |
| 11 | `RequestCacheAwareFilter` | 12 | Khôi phục request ban đầu (sau khi login thành công) — user muốn truy cập `/admin` nhưng bị redirect `/login`, sau login quay lại `/admin`). |
| 12 | `SecurityContextHolderAwareRequestFilter` | 13 | Wrapper request gốc (`HttpServletRequest`) thành `SecurityContextHolderAwareRequestWrapper` — hỗ trợ `request.isUserInRole()`, `request.getRemoteUser()`... |
| 13 | `AnonymousAuthenticationFilter` | 14 | Gán **Authentication ẩn danh** (`ROLE_ANONYMOUS`) cho user chưa đăng nhập — đảm bảo `SecurityContext` không bao giờ null. |
| 14 | `ExceptionTranslationFilter` | 15 | Bắt các exception liên quan đến quyền truy cập → trả về **trang lỗi 403** hoặc redirect **trang login 401**. |
| 15 | `AuthorizationFilter` | 16 | **(Filter cuối cùng)** — Kiểm tra quyền truy cập resource: `hasRole("ADMIN")`, `hasAuthority("READ")`... → cho phép hoặc ném `AccessDeniedException`. |

---

### Sơ đồ luồng request qua 16 filter

```
REQUEST
  │
  ▼
[0] DisableEncodeUrlFilter          ─── Tối ưu URL
  ▼
[1] WebAsyncManagerIntegrationFilter ─── Tích hợp async context
  ▼
[2] SecurityContextHolderFilter     ─── Lưu/khôi phục SecurityContext
  ▼
[3] HeaderWriterFilter              ─── Ghi security headers
  ▼
[4] CsrfFilter                       ─── Kiểm tra CSRF token
  ▼
[5] LogoutFilter                     ─── Xử lý logout (/logout)
  ▼
[6] UsernamePasswordAuthenticationFilter ─── Đăng nhập (form login)
  ▼
[7] DefaultResourcesFilter           ─── Cho phép tài nguyên tĩnh
  ▼
[8] DefaultLoginPageGeneratingFilter ─── Sinh trang login mặc định
  ▼
[9] DefaultLogoutPageGeneratingFilter ─── Sinh trang logout mặc định
  ▼
[10] BasicAuthenticationFilter       ─── Basic Auth (header)
  ▼
[11] RequestCacheAwareFilter         ─── Khôi phục request gốc sau login
  ▼
[12] SecurityContextHolderAwareRequestFilter ─── Wrapper request (isUserInRole...)
  ▼
[13] AnonymousAuthenticationFilter   ─── Gán anonymous role
  ▼
[14] ExceptionTranslationFilter      ─── Xử lý exception 403/401
  ▼
[15] AuthorizationFilter             ─── Kiểm tra quyền cuối cùng ⭐
  │
  ▼
RESPONSE
```

---

### Chi tiết 3 filter quan trọng nhất

#### 1. UsernamePasswordAuthenticationFilter (#6)

Filter trung tâm xử lý **form login**.

```java
// Khi user submit form login (POST /login)
UsernamePasswordAuthenticationFilter
    │
    ├── Đọc username từ request param ("username")
    ├── Đọc password từ request param ("password")
    ├── Tạo object UsernamePasswordAuthenticationToken
    │
    └── Gọi AuthenticationManager.authenticate(token)
             │
             ├── DaoAuthenticationProvider
             │     ├── UserDetailsService.loadUserByUsername() → lấy user từ DB
             │     └── PasswordEncoder.matches(rawPass, encodedPass) → so sánh
             │
             ├── ✅ Thành công → lưu vào SecurityContext → redirect trang gốc
             └── ❌ Thất bại  → ném AuthenticationException → redirect /login?error
```

#### 2. AuthorizationFilter (#15) — *(trước đây là FilterSecurityInterceptor)*

Filter **cuối cùng và quan trọng nhất** — kiểm tra quyền truy cập resource.

```java
AuthorizationFilter
    │
    ├── Lấy Authentication hiện tại từ SecurityContext
    │
    ├── Kiểm tra: SecurityConfig đã khai báo gì cho resource này?
    │
    ├── hasRole("ADMIN")?    → kiểm tra authorities có "ROLE_ADMIN"?
    ├── hasAuthority("READ")? → kiểm tra authorities có "READ"?
    └── hasPermission()?      → kiểm tra quyền chi tiết hơn
    │
    ├── ✅ Pass → cho phép truy cập resource
    └── ❌ Fail
          │
          ├── Đã đăng nhập → ném AccessDeniedException → ExceptionTranslationFilter → 403
          └── Chưa đăng nhập → ném AuthenticationException → ExceptionTranslationFilter → 401 / redirect login
```

#### 3. AnonymousAuthenticationFilter (#13)

Đảm bảo mọi request đều có `SecurityContext` — không bao giờ null.

```java
// Khi chưa đăng nhập, các filter sau vẫn cần đọc Authentication
AnonymousAuthenticationFilter
    │
    └── Gán Authentication ẩn danh:
        Principal = "anonymousUser"
        Authorities = ["ROLE_ANONYMOUS"]
```

→ Nhờ vậy code bên trong có thể luôn gọi `SecurityContextHolder.getContext().getAuthentication()` mà không bị `NullPointerException`.

---

### Thứ tự filter có thể thay đổi không?

Có. Khi cấu hình `HttpSecurity`, thứ tự filter phụ thuộc vào những gì bạn bật/tắt:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())           // ← tắt CsrfFilter (#4)
            .formLogin(form -> form.permitAll())    // ← bật UsernamePasswordAuthFilter + DefaultLoginPageGeneratingFilter
            .httpBasic(basic -> {})                  // ← bật BasicAuthenticationFilter (#10)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

| Tùy chọn | Filter bị ảnh hưởng |
|---|---|
| `csrf().disable()` | `CsrfFilter` (#4) bị tắt |
| `formLogin()` | `UsernamePasswordAuthenticationFilter` (#6), `DefaultLoginPageGeneratingFilter` (#8) |
| `httpBasic()` | `BasicAuthenticationFilter` (#10) |
| `logout()` | `LogoutFilter` (#5) |
| Không có `formLogin()` | `DefaultLoginPageGeneratingFilter` (#8) không có mặt |

---

- Sau khi filter chain xử lý xong, luồng rẽ hai hướng:
  1. **SecurityContextHolder** — lưu trữ context (Principal + Authorities)
  2. **Authentication Manager** — xử lý xác thực

### 2. SecurityContextHolder

```
┌─────────────────────────────────┐
│       SECURITY CONTEXT HOLDER   │
│  ┌────────────┐ ┌────────────┐ │
│  │ Authorities│ │ Principal  │ │
│  └────────────┘ └────────────┘ │
└─────────────────────────────────┘
```

- Lưu trữ thông tin bảo mật của người dùng **sau khi xác thực thành công**.
- **Principal**: Tên người dùng (username) hoặc đối tượng user details.
- **Authorities**: Danh sách quyền/role của người dùng (ví dụ: `ROLE_USER`, `ROLE_ADMIN`).
- Được lưu trong `SecurityContext`, và có thể truy cập ở bất kỳ đâu trong ứng dụng qua:
  ```java
  SecurityContextHolder.getContext().getAuthentication()
  ```

### 3. Authentication Manager

```
Security Filter Chain  ──▶  Authentication Manager
                              │
                  ┌───────────┼───────────┐
                  ▼           ▼           ▼
             Provider 1   Provider 2   Provider 3
```

- **Authentication Manager**: Giao diện trung tâm điều phối việc xác thực.
- Ủy quyền (delegate) việc xác thực cho một hoặc nhiều **Authentication Provider**.
- Phương thức chính: `Authentication authenticate(Authentication request)`

### 4. Authentication Provider (1, 2, 3)

- Mỗi provider thực hiện một **logic xác thực cụ thể**:
  - `DaoAuthenticationProvider` — xác thực bằng database (dùng `UserDetailsService`)
  - `JwtAuthenticationProvider` — xác thực bằng JWT token
  - `OAuth2AuthenticationProvider` — xác thực qua OAuth2 (Google, GitHub...)
  - `LdapAuthenticationProvider` — xác thực qua LDAP
- Provider 2 (trong sơ đồ) tương tác với:
  1. **Password Encoder** — mã hóa / so sánh mật khẩu
  2. **User Details Service** — tải thông tin người dùng

### 5. Password Encoder

- Chịu trách nhiệm **mã hóa mật khẩu** và **kiểm tra mật khẩu**.
- Các loại phổ biến:
  - `BCryptPasswordEncoder` — thuật toán BCrypt (**khuyến nghị**)
  - `NoOpPasswordEncoder` — không mã hóa (test)
  - `StandardStringPasswordEncoder` — SHA-256
- Ví dụ sử dụng:
  ```java
  // Mã hóa
  String encoded = passwordEncoder.encode("myPassword");

  // Kiểm tra
  boolean matches = passwordEncoder.matches("myPassword", encodedHash);
  ```

### 6. User Details Service

- Giao diện tải thông tin người dùng từ **database** hoặc nguồn khác.
- Phương thức chính: `UserDetails loadUserByUsername(String username)`
- Tương tác với **Docker (DBS)** (database) để truy vấn thông tin người dùng.
- Ví dụ:
  ```java
  @Service
  public class CustomUserDetailsService implements UserDetailsService {

      @Autowired
      private UserRepository userRepository;

      @Override
      public UserDetails loadUserByUsername(String username)
              throws UsernameNotFoundException {
          User user = userRepository.findByUsername(username)
              .orElseThrow(() -> new UsernameNotFoundException(...));
          return user;
      }
  }
  ```

### 7. Docker (DBS)

- Hệ thống database lưu trữ thông tin người dùng (username, password đã mã hóa, roles...).
- `User Details Service` truy vấn database để lấy dữ liệu phục vụ xác thực.

---

## Luồng xác thực tổng hợp

```
1.  User Request
        │
        ▼
2.  Security Filter Chain
        │  (filter 1 → filter 2 → filter 3 → ... → filter N)
        ▼
3a. SecurityContextHolder  ◄── lưu Principal + Authorities
        │
3b. Authentication Manager  ◄── ủy quyền xác thực
        │
        ▼
4.  Authentication Provider (1 / 2 / 3)
        │
        ├── Password Encoder ──── kiểm tra mật khẩu
        │
        └── User Details Service ──── truy vấn DB
                 │
                 ▼
           Docker (DBS)
                 │
        (trả về UserDetails)
                 │
        ▼
5.  Xác thực thành công → SecurityContextHolder (Principal + Authorities)
                 │
                 ▼
6.  User Response
```

---

## Code mẫu SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout.permitAll());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Các phương thức xác thực (Authentication Methods)

### 1. Basic Authentication

Xác thực HTTP Basic — gửi thông tin đăng nhập qua **header** `Authorization`.

**Cơ chế:**
- Client gửi request kèm header:
  ```
  Authorization: Basic base64(username:password)
  ```
- Server giải mã Base64 → lấy username/password → so sánh với database.
- Nếu đúng → cho phép truy cập; sai → trả về `401 Unauthorized` + header `WWW-Authenticate: Basic`.

**Ưu điểm:** Đơn giản, hỗ trợ rộng rãi (trình duyệt, HTTP client, curl...).

**Nhược điểm:** Username/password gửi dưới dạng Base64 (**không mã hóa**). → Phải dùng qua **HTTPS** để đảm bảo bảo mật.

**Cấu hình trong Spring Security:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {});          // ← bật Basic Auth
        return http.build();
    }
}
```

**Dùng với Postman / curl:**
```bash
curl -u username:password http://localhost:8080/api/resource
```

---

### 2. Digest Authentication

Xác thực HTTP Digest — cải tiến hơn Basic, **không gửi mật khẩu dạng plain text**.

**Cơ chế:**
1. Client gửi request **không có** thông tin xác thực.
2. Server phản hồi `401 Unauthorized` + header `WWW-Authenticate: Digest` kèm **nonce** (chuỗi ngẫu nhiên server-side) và **realm** (vùng xác thực).
3. Client tính toán **hash** từ:
   ```
   hash(username : realm : password) + nonce + HTTP method + URI
   ```
   → gửi kết quả lên server dưới dạng `Authorization: Digest ...`
4. Server kiểm tra hash → xác thực thành công hoặc thất bại.

**Ưu điểm:** Mật khẩu không gửi trực tiếp qua mạng, chống replay attack (nhờ nonce).

**Nhược điểm:**
- Phức tạp hơn Basic Auth.
- Cần server lưu mật khẩu dạng **plain text hoặc MD5** (để tính hash) → kém bảo mật hơn khi database bị lộ.
- Hầu hết trình duyệt **không hỗ trợ tốt**.

**Cấu hình trong Spring Security:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .digestAuthentication(digest -> {});   // ← bật Digest Auth
        return http.build();
    }
}
```

---

### 3. SSL Client Authentication (X.509 Certificate)

Xác thực bằng **chứng chỉ số (certificate)** phía client — còn gọi là **mutual TLS (mTLS)**.

**Cơ chế:**
1. Server có **SSL/TLS certificate** (chứng chỉ phía server).
2. Client cũng có **X.509 certificate** (chứng chỉ phía client) được cấp phát trước.
3. Khi handshake TLS:
   - Server gửi certificate → client xác minh (để xác thực **server**).
   - Client gửi certificate → server xác minh (để xác thực **client**).
4. Nếu certificate hợp lệ → kết nối được thiết lập → user được xác thực dựa trên **DN (Distinguished Name)** trong certificate.

**Ưu điểm:**
- Rất bảo mật — không cần password.
- Không thể giả mạo certificate (dựa trên PKI).
- Phù hợp cho **microservices**, **IoT**, **doanh nghiệp** (B2B).

**Nhược điểm:**
- Cần infrastructure PKI (Public Key Infrastructure).
- Phát hành và quản lý certificate phức tạp.
- Không thuận tiện cho người dùng cuối (end-user).

**Cấu hình trong Spring Security:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .x509(x509 -> x509
                .subjectPrincipalRegex("CN=(.*)")   // ← extract username từ certificate DN
                .userDetailsService(userDetailsService())
            );
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Load user từ certificate CN
        return username -> User.builder()
            .username(username)
            .password("")       // không cần password vì đã xác thực qua certificate
            .roles("USER")
            .build();
    }
}
```

**Cấu hình SSL trong `application.properties`:**
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.client-auth=need        # need = bắt buộc, want = khuyến khích
server.ssl.trust-store=classpath:truststore.p12
server.ssl.trust-store-password=changeit
```

---

### 4. Form-based Authentication

Xác thực bằng **form HTML** do ứng dụng web sinh ra.

**Cơ chế:**
1. User truy cập trang cần xác thực → server redirect đến **trang login** (`/login`).
2. User nhập username/password → gửi **POST** request đến `/login`.
3. `UsernamePasswordAuthenticationFilter` thu thập thông tin → gọi `AuthenticationManager`.
4. `AuthenticationProvider` (thường là `DaoAuthenticationProvider`) kiểm tra:
   - `UserDetailsService.loadUserByUsername()` → lấy user từ DB.
   - `PasswordEncoder.matches()` → so sánh mật khẩu.
5. Thành công → lưu vào `SecurityContext` → redirect đến trang ban đầu.
6. Thất bại → redirect đến `/login?error`.

**Ưu điểm:**
- Giao diện người dùng tùy chỉnh được (custom login page).
- Dễ tích hợp CAPTCHA, "Remember Me", social login...
- Phổ biến nhất cho ứng dụng web thông thường.

**Nhược điểm:**
- Phụ thuộc trình duyệt / HTML form.
- Cần bảo vệ trang login khỏi brute-force attack.

**Cấu hình trong Spring Security:**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")           // trang login tùy chỉnh
                .loginProcessingUrl("/login")  // URL xử lý POST
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }
}
```

**Template HTML (JSP / Thymeleaf):**
```html
<!-- Thymeleaf -->
<form th:action="@{/login}" method="post">
    <div th:if="${param.error}">Sai tài khoản hoặc mật khẩu</div>
    <div th:if="${param.logout}">Đã đăng xuất</div>

    <input type="text" name="username" placeholder="Username" required />
    <input type="password" name="password" placeholder="Password" required />
    <button type="submit">Đăng nhập</button>
</form>
```

---

## So sánh 4 phương thức xác thực

| Tiêu chí | Basic Auth | Digest Auth | SSL Client Auth | Form-based Auth |
|---|---|---|---|---|
| **Thông tin gửi đi** | Base64 (username:pass) | MD5/SHA hash | X.509 Certificate | Username/password POST |
| **Mã hóa** | Không (cần HTTPS) | Hash (không gửi plain text) | Mã hóa TLS | Không (cần HTTPS) |
| **Bảo mật** | Thấp | Trung bình | Rất cao | Trung bình |
| **Độ phức tạp** | Đơn giản | Trung bình | Phức tạp (PKI) | Đơn giản |
| **Phạm vi sử dụng** | API / CLI / Test | Ít phổ biến | Doanh nghiệp / Microservices | Ứng dụng web |
| **User experience** | Hộp thoại trình duyệt | Hộp thoại trình duyệt | Tự động (cert) | Trang login tùy chỉnh |
| **Cần server setup** | Không | Không | SSL Certificate | Không |
| **Chống replay attack** | Không | Có (nhờ nonce) | Có (TLS) | Không (cần thêm) |

---

## Các cách Custom User trong Spring Security

Có nhiều cách để khai báo và quản lý user trong Spring Security. Dưới đây là **2 cách phổ biến nhất**.

---

### C1: Cấu hình trong SecurityConfig

Khai báo user trực tiếp bên trong class `SecurityConfig`. Phù hợp khi số lượng user ít, **ít thay đổi** (ví dụ: môi trường dev, test, demo).

**Cơ chế:**
- Dùng `UserDetailsManager` (cụ thể là `InMemoryUserDetailsManager`) để lưu user **trong RAM**.
- Mỗi user được tạo bằng `User.builder()`, mật khẩu phải được **mã hóa** qua `PasswordEncoder`.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.permitAll())
            .logout(logout -> logout.permitAll());
        return http.build();
    }

    // ===== CẤU HÌNH USER =====
    @Bean
    public UserDetailsManager userDetailsManager() {
        // InMemoryUserDetailsManager — lưu trong RAM
        return new InMemoryUserDetailsManager(
            User.builder()
                .username("user")
                .password(passwordEncoder().encode("123456"))
                .roles("USER")
                .build(),

            User.builder()
                .username("admin")
                .password(passwordEncoder().encode("admin123"))
                .roles("ADMIN", "USER")
                .build()
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Lưu ý:**
- Nếu không khai báo `UserDetailsManager` mà chỉ khai báo `UserDetailsService`, Spring Security sẽ tự dùng `InMemoryUserDetailsManager`.
- Mật khẩu **bắt buộc phải encode** — nếu để plain text sẽ bị lỗi启动.

**Ưu điểm:** Nhanh, đơn giản, không cần database.

**Nhược điểm:**
- User được lưu trong RAM → **mất khi restart** ứng dụng.
- Không phù hợp cho production với nhiều user.
- Khó quản lý khi user có nhiều thuộc tính tùy chỉnh (email, phone, avatar...).

---

### C2: Custom UserDetailsService

Tạo class triển khai interface `UserDetailsService` để **tải user từ database** hoặc bất kỳ nguồn nào. Đây là cách **phổ biến và mạnh mẽ nhất** trong production.

**Cơ chế:**
1. `UserDetailsService.loadUserByUsername()` được gọi bởi `DaoAuthenticationProvider`.
2. Trong method, truy vấn database tìm user theo username.
3. Trả về đối tượng `UserDetails` chứa thông tin: username, password (đã mã hóa), authorities.
4. `DaoAuthenticationProvider` so sánh password gửi lên với password trong DB.

#### Bước 1 — Tạo Entity User

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;
    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;

    @ElementCollection(fetch = FetchType.EAGER)    // load roles ngay khi load user
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();
}
```

#### Bước 2 — Tạo Repository

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

#### Bước 3 — Triển khai CustomUserDetailsService

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // Tìm user trong database
        User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("Không tìm thấy user: " + username)
            );

        // Chuyển đổi Entity User → UserDetails
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())   // đã được mã hóa sẵn trong DB
            .disabled(!user.isEnabled())
            .accountExpired(!user.isAccountNonExpired())
            .accountLocked(!user.isAccountNonLocked())
            .credentialsExpired(!user.isCredentialsNonExpired())
            .authorities(user.getRoles().toArray(new String[0]))   // ["ROLE_USER", "ROLE_ADMIN"]
            .build();
    }
}
```

#### Bước 4 — Cấu hình SecurityConfig sử dụng CustomUserDetailsService

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout.permitAll());
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Ưu điểm:**
- User được lưu trong database → **persist qua restart**.
- Dễ dàng mở rộng với nhiều user, nhiều roles.
- Có thể tùy chỉnh đầy đủ thông tin user (email, avatar, trạng thái tài khoản...).
- Dễ dàng kết hợp với JPA, MySQL, PostgreSQL...

**Nhược điểm:**
- Cần cài đặt database.
- Nhiều code hơn so với C1.

---

### So sánh C1 vs C2

| Tiêu chí | C1: SecurityConfig | C2: CustomUserDetailsService |
|---|---|---|
| **Nơi lưu user** | RAM (InMemory) | Database |
| **Tồn tại sau restart** | ❌ Mất | ✅ Có |
| **Số lượng user** | Ít (1–5) | Không giới hạn |
| **Độ phức tạp** | Thấp | Trung bình |
| **Phạm vi** | Dev, Test, Demo | Development → Production |
| **Mở rộng thông tin user** | Không | Có (email, phone, avatar...) |
| **Quản lý role** | Đơn giản | Linh hoạt |
| **Thích hợp cho** | MVP, prototype | Ứng dụng thực tế |

---

### Bonus: Mã hóa password khi lưu vào Database

Khi user đăng ký hoặc admin tạo user mới, cần mã hóa password trước khi lưu:

```java
@Autowired
private PasswordEncoder passwordEncoder;

// Khi tạo user mới
public void createUser(String username, String rawPassword, Set<String> roles) {
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(rawPassword));  // ← MÃ HÓA Ở ĐÂY
    user.setRoles(roles);
    user.setEnabled(true);
    userRepository.save(user);
}
```

---

## Phân biệt `.roles()` và `.authorities()` trong Spring Security

### Hai cách viết tương đương

```java
// Cách 1: dùng .roles() — Spring Security tự thêm prefix "ROLE_"
User.builder()
    .username("admin")
    .password(passwordEncoder.encode("admin123"))
    .roles("USER", "ADMIN")          // → Authorities: ["ROLE_USER", "ROLE_ADMIN"]
    .build();

// Cách 2: dùng .authorities() — khai báo trực tiếp, không thêm prefix
User.builder()
    .username("admin")
    .password(passwordEncoder.encode("admin123"))
    .authorities("ROLE_USER", "ROLE_ADMIN")  // → Authorities: ["ROLE_USER", "ROLE_ADMIN"]
    .build();
```

→ **Kết quả cuối cùng hoàn toàn giống nhau.** Cả hai đều sinh ra danh sách `GrantedAuthority` là `["ROLE_USER", "ROLE_ADMIN"]`.

---

### Tại sao lại có `.roles()` ?

Spring Security quy ước: **Role** = Authority có prefix `ROLE_`.

```
.roles("ADMIN")              →  Authority = "ROLE_ADMIN"
.roles("USER", "ADMIN")     →  Authorities = ["ROLE_USER", "ROLE_ADMIN"]
```

Khi dùng `.roles()`, Spring Security **tự động thêm** prefix `ROLE_` vào mỗi phần tử. Đây chỉ là **shortcut** — viết nhanh hơn, không phải gõ `ROLE_` thủ công.

Khi dùng `.authorities()`, bạn phải **tự thêm prefix** `ROLE_` nếu muốn nó được coi là role.

---

### Khi nào dùng `.roles()` ? Khi nào dùng `.authorities()` ?

| Trường hợp | Nên dùng | Lý do |
|---|---|---|
| Khai báo role đơn giản | `.roles("ADMIN")` | Ngắn gọn, không cần nhớ prefix |
| Khai báo permission cụ thể | `.authorities("READ", "WRITE")` | Không phải role → không có prefix `ROLE_` |
| Role + Permission trộn lẫn | `.roles("USER").authorities("READ", "WRITE")` | Kết hợp cả hai |
| Kiểm tra trong `hasRole()` | role = `"ADMIN"` (không có ROLE_) | `hasRole("ADMIN")` → kiểm tra `"ROLE_ADMIN"` |
| Kiểm tra trong `hasAuthority()` | authority = `"READ"` | `hasAuthority("READ")` → kiểm tra chính xác `"READ"` |

---

### Ví dụ kết hợp Role + Permission

```java
// User thường: có role USER + permission đọc/ghi
User.builder()
    .username("user")
    .password(passwordEncoder.encode("123456"))
    .roles("USER")
    .authorities("READ", "WRITE")
    .build();
// → Authorities: ["ROLE_USER", "READ", "WRITE"]

// Admin: có role ADMIN + USER + tất cả permission
User.builder()
    .username("admin")
    .password(passwordEncoder.encode("admin123"))
    .roles("ADMIN", "USER")
    .authorities("READ", "WRITE", "DELETE")
    .build();
// → Authorities: ["ROLE_ADMIN", "ROLE_USER", "READ", "WRITE", "DELETE"]
```

---

### Ví dụ kiểm tra quyền trong SecurityConfig

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/admin/**").hasRole("ADMIN")        // kiểm tra "ROLE_ADMIN"
        .requestMatchers("/user/**").hasRole("USER")          // kiểm tra "ROLE_USER"
        .requestMatchers("/api/data").hasAuthority("READ")     // kiểm tra authority "READ"
        .requestMatchers("/api/delete").hasAuthority("DELETE")
        .anyRequest().authenticated()
    );
```

| Phương thức | Kiểm tra prefix `ROLE_`? | Ví dụ |
|---|---|---|
| `hasRole("ADMIN")` | ✅ Tự thêm `ROLE_` → so sánh `"ROLE_ADMIN"` | Chỉ dùng cho **role** |
| `hasAuthority("READ")` | ❌ Không thêm → so sánh `"READ"` | Dùng cho **permission** |

---

### Tóm tắt

```
.authorities("ROLE_ADMIN")
    = .roles("ADMIN")
    = hasRole("ADMIN")
    = hasAuthority("ROLE_ADMIN")
```

**Quy tắc nhớ:**
- `hasRole()` → truyền vào **không có** `ROLE_` → Spring tự thêm.
- `hasAuthority()` → truyền vào **đúng** giá trị authority cần kiểm tra.
- `.roles("X")` → tự thêm `ROLE_` → sinh authority `"ROLE_X"`.
- `.authorities("X")` → giữ nguyên → sinh authority `"X"`.

---

### Role và Authority — thằng nào "mạnh" hơn?

**Câu trả lời ngắn: Không có thằng nào mạnh hơn.** Cả hai đều implement interface `GrantedAuthority`, hoàn toàn tương đương về mặt cơ chế kiểm tra.

```java
// Đây là cách Spring Security lưu trữ — cả role và authority đều là GrantedAuthority
Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

// Kiểm tra bằng hasRole() hay hasAuthority() đều kiểm tra cùng một danh sách
auth.hasRole("ADMIN")       // kiểm tra trong danh sách có "ROLE_ADMIN"?
auth.hasAuthority("ADMIN")  // kiểm tra trong danh sách có "ADMIN"?
```

**Sự khác biệt chỉ nằm ở QUY ƯỚC ĐẶT TÊN và CÁCH DÙNG:**

```
.authorities("ROLE_ADMIN")    ← authority tên là "ROLE_ADMIN"
.hasAuthority("ROLE_ADMIN")   ← kiểm tra đúng chuỗi "ROLE_ADMIN"

.roles("ADMIN")               ← sinh authority tên là "ROLE_ADMIN"
.hasRole("ADMIN")             ← tự thêm ROLE_ → kiểm tra "ROLE_ADMIN"

hasRole("ADMIN") == hasAuthority("ROLE_ADMIN")   ← cùng kiểm tra một thằng
```

**Vậy khi nào dùng cái nào cho hợp lý?**

| Ngữ cảnh | Dùng | Lý do |
|---|---|---|
| Kiểm tra **nhóm quyền lớn** (admin, user, editor...) | `hasRole()` | Đọc dễ hiểu, quy ước chuẩn Spring |
| Kiểm tra **quyền cụ thể** (READ, WRITE, DELETE) | `hasAuthority()` | Rõ ràng, không nhầm prefix |
| Mở rộng bằng **Method Security** | `hasAuthority()` hoặc `hasPermission()` | Linh hoạt hơn với `@PreAuthorize` |

```java
// Ví dụ thực tế: dùng kết hợp
@PreAuthorize("hasRole('ADMIN') or hasAuthority('MANAGE_USERS')")
public void manageUsers() { ... }
// → Admin luôn được phép (qua role)
// → User thường chỉ được phép nếu có permission "MANAGE_USERS"
```

**Tóm lại:**
- **Role** ≈ **Authority có prefix `ROLE_`** → dùng cho phân quyền **nhóm người dùng** (admin, user, manager).
- **Authority** → dùng cho phân quyền **hành động cụ thể** (read, write, delete).
- **Không có thằng nào mạnh hơn** — chúng cùng được lưu trong một danh sách và kiểm tra bằng cùng một cơ chế. Chỉ khác nhau ở **quy ước đặt tên** và **mức độ chi tiết**.

---

## Các mô hình phân quyền (Authorization Models)

Khi nói đến **xác minh quyền** (authorization), có 4 mô hình phổ biến: **RBAC, ABAC, DAC, MAC**. Spring Security hỗ trợ tất cả.

---

### 1. RBAC — Role-Based Access Control (Phân quyền theo vai trò)

**Khái niệm:** Người dùng được gán **vai trò (role)**, vai trò quyết định quyền truy cập. Không cần gán quyền trực tiếp cho từng user.

```
User  ──belongs to──▶  Role  ──grants──▶  Permission
                        ▲
                        │
                   User  ──belongs to──▶  Role
```

**Ví dụ thực tế:**
- **Admin** → toàn quyền (đọc, ghi, xóa, quản lý user)
- **Editor** → đọc, ghi bài viết
- **Viewer** → chỉ đọc

**Trong Spring Security:**

```java
// Cấu hình RBAC
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/editor/**").hasAnyRole("ADMIN", "EDITOR")
            .requestMatchers("/user/**").hasRole("USER")
            .anyRequest().authenticated()
        );
    return http.build();
}

// Gán role cho user
@Bean
public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager(
        User.builder()
            .username("admin")
            .password(passwordEncoder.encode("123"))
            .roles("ADMIN")
            .build(),
        User.builder()
            .username("editor")
            .password(passwordEncoder.encode("123"))
            .roles("EDITOR")
            .build(),
        User.builder()
            .username("viewer")
            .password(passwordEncoder.encode("123"))
            .roles("VIEWER")
            .build()
    );
}
```

**Ưu điểm:**
- Đơn giản, dễ quản lý
- Phù hợp hầu hết ứng dụng
- Dễ kiểm toán (audit)

**Nhược điểm:**
- Không linh hoạt khi cần phân quyền theo **ngữ cảnh** (thời gian, địa điểm...)
- Không hỗ trợ quyền theo **resource cụ thể**

---

### 2. ABAC — Attribute-Based Access Control (Phân quyền theo thuộc tính)

**Khái niệm:** Quyền truy cập được quyết định dựa trên **nhiều thuộc tính** của user, resource, và ngữ cảnh — không chỉ riêng vai trò.

```
Quyền truy cập = f(
    user attributes  (age, department, location, time),
    resource attributes (owner, sensitivity, type),
    environment attributes (IP, time, device)
)
```

**Ví dụ thực tế:**
- "Chỉ manager mới được duyệt chi phí > 10 triệu"
- "User chỉ được sửa bài viết **của chính mình**"
- "Không cho phép truy cập sau 22:00"

**Trong Spring Security — dùng `@PreAuthorize`:**

```java
// Kích hoạt Method Security
@EnableMethodSecurity
@Configuration
public class SecurityConfig { ... }
```

```java
@Service
public class ExpenseService {

    // Ví dụ ABAC: chỉ manager mới được duyệt chi phí > 10 triệu
    @PreAuthorize("@expenseService.canApprove(#amount, authentication)")
    public void approveExpense(double amount) {
        // ...
    }

    // Kiểm tra nhiều thuộc tính cùng lúc
    @PreAuthorize("hasRole('EDITOR') and " +
                  "(#post.author == authentication.name or hasRole('ADMIN'))")
    public void editPost(Post post) {
        // Editor chỉ sửa được bài của mình; Admin sửa được mọi bài
    }

    // ABAC với điều kiện thời gian
    @PreAuthorize("hasRole('EMPLOYEE') and " +
                  "T(java.time.LocalTime).now().isBefore(T(java.time.LocalTime).of(22, 0))")
    public void accessLateShiftResource() {
        // Chỉ cho phép trước 22:00
    }
}
```

**Cấu hình SpEL phức tạp hơn với `@PostAuthorize`:**

```java
@PostAuthorize("returnObject.owner == authentication.name or hasRole('ADMIN')")
public Document getDocument(Long id) {
    // Lấy document, sau đó kiểm tra — user chỉ thấy document của mình
    return documentRepository.findById(id).orElseThrow();
}
```

**So sánh:**
- `@PreAuthorize` — kiểm tra **TRƯỚC KHI** thực thi method
- `@PostAuthorize` — kiểm tra **SAU KHI** method trả kết quả (dùng `returnObject`)

**Ưu điểm:**
- Linh hoạt, chi tiết
- Hỗ trợ ngữ cảnh (thời gian, vị trí, resource cụ thể)
- Mạnh mẽ cho enterprise

**Nhược điểm:**
- Phức tạp hơn RBAC
- Cần hiểu SpEL (Spring Expression Language)
- Khó kiểm toán khi rules phức tạp

---

### 3. DAC — Discretionary Access Control (Phân quyền tự do)

**Khái niệm:** Chủ sở hữu resource có thể **tự quyết định** ai được truy cập resource đó. Không có authority trung tâm.

```
Ví dụ: Google Drive
  - File của tôi → tôi có thể share cho ai tôi muốn
  - Tôi là owner → tôi quyết ai được đọc, ai được ghi
```

**Trong Spring Security:**

```java
// Mô phỏng DAC: kiểm tra chủ sở hữu resource
@PreAuthorize("hasRole('USER')")
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    // User chỉ được cập nhật document CỦA MÌNH
    @PreAuthorize("@documentService.isOwner(#docId, authentication.name)")
    public void updateDocument(Long docId, String content) {
        Document doc = documentRepository.findById(docId)
            .orElseThrow(() -> new AccessDeniedException("Không có quyền"));
        doc.setContent(content);
        documentRepository.save(doc);
    }

    // User có thể share document cho người khác
    public void shareDocument(Long docId, String targetUsername, String role) {
        Document doc = documentRepository.findById(docId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy"));

        // Chỉ owner mới được share
        if (!doc.getOwner().equals(SecurityContextHolder.getContext()
                .getAuthentication().getName())) {
            throw new AccessDeniedException("Chỉ owner mới được share");
        }

        documentAccessRepository.save(new DocumentAccess(docId, targetUsername, role));
    }
}
```

**Entity mô phỏng bảng phân quyền DAC:**

```java
@Entity
public class DocumentAccess {
    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "grantee_username")
    private String granteeUsername;  // người được chia sẻ

    private String accessLevel;      // READ, WRITE, ADMIN
    private LocalDateTime grantedAt;
}
```

**Ưu điểm:**
- Linh hoạt cho người dùng cuối
- Phù hợp ứng dụng cộng tác (Google Drive, Notion, Dropbox...)

**Nhược điểm:**
- Khó kiểm soát tập trung
- Rủi ro bảo mật nếu user share quá rộng
- Khó audit

---

### 4. MAC — Mandatory Access Control (Phân quyền bắt buộc)

**Khái niệm:** Quyền truy cập được **quy định bắt buộc** bởi hệ thống/quản trị viên. User không thể tự thay đổi quyền của mình.

```
Ví dụ:
  - Hệ thống quân sự: "Bí mật" chỉ đọc được bởi cấp "Bí mật" trở lên
  - SELinux (Linux): kernel quyết định tất cả
  -隔 (隔)隔 (隔)
```

**Trong Spring Security:**

```java
// MAC: quyền do hệ thống/quản trị viên kiểm soát hoàn toàn
// User không thể tự ý thay đổi quyền của mình

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // MAC: mọi quyền được kiểm tra cứng — không có user nào tự ý sửa
                .requestMatchers("/classified/**").hasAuthority("CLEARANCE_LEVEL_3")
                .requestMatchers("/secret/**").hasAuthority("CLEARANCE_LEVEL_2")
                .requestMatchers("/topsecret/**").hasAuthority("CLEARANCE_LEVEL_1")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

**Kiểm tra clearance cấp độ (MAC):**

```java
@Service
public class ClearanceService {

    public boolean hasClearanceLevel(Authentication auth, int requiredLevel) {
        // Security Officer (quản trị viên) gán clearance cứng — user không thể tự ý thay đổi
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> a.startsWith("CLEARANCE_LEVEL_"))
            .map(a -> Integer.parseInt(a.replace("CLEARANCE_LEVEL_", "")))
            .anyMatch(level -> level <= requiredLevel);  // cấp càng thấp → quyền càng cao
    }
}

@PreAuthorize("@clearanceService.hasClearanceLevel(authentication, 2)")
public void accessSecretDocument() {
    // Chỉ clearance cấp 2 (CLEARANCE_LEVEL_2) trở xuống mới được vào
}
```

**Ưu điểm:**
- Bảo mật cao nhất
- Kiểm soát tập trung hoàn toàn
- Phù hợp quân sự, chính phủ, tài chính

**Nhược điểm:**
- Ít linh hoạt
- Phức tạp trong triển khai

---

### So sánh 4 mô hình phân quyền

| Tiêu chí | RBAC | ABAC | DAC | MAC |
|---|---|---|---|---|
| **Quyết định quyền** | Vai trò | Nhiều thuộc tính | Chủ sở hữu resource | Hệ thống/quản trị |
| **Linh hoạt** | Trung bình | Cao | Rất cao | Thấp |
| **Độ phức tạp** | Thấp | Cao | Trung bình | Cao |
| **Dễ kiểm toán** | Dễ | Khó | Trung bình | Rất dễ |
| **Phù hợp** | Ứng dụng thông thường | Enterprise, SaaS | Ứng dụng cộng tác | Quân sự, chính phủ |
| **Ví dụ** | Admin / User / Editor | "Chỉ manager > 10M" | Google Drive, Dropbox | SELinux, military |

---

### Kết hợp RBAC + ABAC trong Spring Security (thực tế nhất)

Đa số ứng dụng thực tế dùng **RBAC làm nền tảng + ABAC bổ sung** cho ngữ cảnh:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // ===== RBAC: phân quyền theo vai trò =====
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/admin/**").hasRole("ADMIN")
            .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers("/user/**").hasRole("USER")
            .requestMatchers("/public/**").permitAll()
            .anyRequest().authenticated()
        )
        // ===== ABAC: kiểm tra ngữ cảnh =====
        .formLogin(form -> form.permitAll());
    return http.build();
}
```

```java
@EnableMethodSecurity
public class AppConfig { }

// Trong service
@PreAuthorize("hasRole('USER') and " +
              "(#resource.owner == authentication.name or hasRole('ADMIN'))")
public void modifyResource(Resource resource) {
    // RBAC: user mới được vào đây
    // ABAC: chỉ owner hoặc admin mới được sửa resource cụ thể này
}
```

---

## Bảng tổng hợp màu sắc sơ đồ

| Màu | Thành phần | Vai trò |
|---|---|---|
| 🟢 Xanh lá | User Request / Response | Điểm đầu vào và đầu ra của luồng |
| 🟠 Cam / ⬛ Đen | Security Filter Chain | Chuỗi bộ lọc bảo mật |
| 🔴 Đỏ / 🩷 Hồng | SecurityContextHolder | Lưu Principal + Authorities |
| 🟡 Vàng | Authentication Manager | Điều phối xác thực |
| 🩷 Hồng | Authentication Provider (1, 2, 3) | Thực hiện logic xác thực |
| 🔵 Xanh dương nhạt | Password Encoder, User Details Service, Docker (DBS) | Dịch vụ hỗ trợ xác thực |

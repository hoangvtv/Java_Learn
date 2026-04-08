# JPA / Hibernate Configuration

## Mục lục

- [1. Cấu hình đầy đủ](#1-cấu-hình-đầy-đủ)
- [2. Giải thích chi tiết](#2-giải-thích-chi-tiết)
  - [2.1 `spring.application.name`](#21-springapplicationname)
  - [2.2 `datasource`](#22-datasource)
  - [2.3 `jpa.hibernate.ddl-auto`](#23-jpahibernateddl-auto)
  - [2.4 `jpa.show-sql`](#24-jpashow-sql)
  - [2.5 `jpa.properties.hibernate.*`](#25-jpapropertieshibernate)
- [3. Entity Annotations](#3-entity-annotations)
  - [3.6 `@GeneratedValue` — chiến lược tạo ID](#36-generatedvalue--chiến-lược-tạo-id)
- [4. Repository — Các cách viết Query](#4-repository--các-cách-viết-query)
  - [4.1 Built-in Methods (CrudRepository / JpaRepository)](#41-built-in-methods--crudrepository--jparepository)
  - [4.2 Method Name (Query Derivation)](#42-method-name--query-derivation)
  - [4.3 JPQL và Native Query](#43-jpql-và-native-query)
  - [4.4 Named Query](#44-named-query)
  - [4.5 Criteria API / Specification](#45-criteria-api--specification)
  - [4.6 QueryDSL](#46-querydsl)
  - [4.7 `@Modifying` và `@Transactional`](#47-modifying-và-transactional)
- [5. Quan hệ Entity — Cascade, JoinColumn, Relationships](#5-quan-hệ-entity--cascade-joincolumn-relationships)
  - [5.4 `orphanRemoval` — xóa con khi bị gỡ khỏi cha](#54-orphanremoval--xóa-con-khi-bị-gỡ-khỏi-cha)
  - [4.7 `@Modifying` và `@Transactional`](#47-modifying-và-transactional)

---

## 1. Cấu hình đầy đủ

```yaml
spring:
  application:
    name: spring_basic
  datasource:
    url: jdbc:mysql://localhost:3306/spring_basic
    username: root
    password: [PASSWORD]
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        use_sql_comments: true
        generate_statistics: true
        highlight_sql: true
        show_sql: true
```

## 2. Giải thích chi tiết

### 2.1 `spring.application.name`

Tên ứng dụng, dùng để hiển thị trong Spring Boot Actuator, Eureka, và các công cụ quản lý.

```yaml
spring:
  application:
    name: spring_basic   # Tên ứng dụng
```

### 2.2 `datasource`

Cấu hình kết nối đến database MySQL.

```yaml
datasource:
  url: jdbc:mysql://localhost:3306/spring_basic   # Chuỗi kết nối JDBC
  username: root                                    # Tài khoản MySQL
  password: [PASSWORD]                              # Mật khẩu MySQL
  driver-class-name: com.mysql.cj.jdbc.Driver      # Driver JDBC cho MySQL 8+
```

| Thuộc tính | Ý nghĩa |
|------------|---------|
| `url` | Chuỗi kết nối JDBC tới MySQL server |
| `username` | Tên đăng nhập MySQL |
| `password` | Mật khẩu đăng nhập MySQL |
| `driver-class-name` | Driver dùng cho MySQL 8+ (thay driver cũ `com.mysql.jdbc.Driver`) |

> **Lưu ý bảo mật:** Không hard-code password trực tiếp. Nên dùng biến môi trường hoặc file `application-secrets.yaml` không commit lên git.

### 2.3 `jpa.hibernate.ddl-auto`

Quan trọng nhất — kiểm soát cách JPA/Hibernate tự động tạo hoặc cập nhật bảng trong database.

| Giá trị        | Hành vi                                                                 |
|----------------|-------------------------------------------------------------------------|
| `none`         | Không làm gì — tắt hoàn toàn                                            |
| `validate`     | Chỉ kiểm tra schema có đúng với entity không, **không thay đổi** gì    |
| `update`       | Cập nhật schema nếu entity thay đổi, **giữ nguyên dữ liệu cũ**         |
| `create`       | Tạo lại bảng mới **mỗi lần chạy**, xóa hết dữ liệu cũ                  |
| `create-drop`  | Tạo bảng khi ứng dụng start, xóa khi stop                               |

> **Khuyến nghị theo môi trường:**
> - **Dev:** `update` — thoải mái thêm field mà không mất data
> - **Test:** `create` hoặc `create-drop` — cần database sạch mỗi lần chạy
> - **Production:** `none` hoặc `validate` — dùng migration tool (Flyway, Liquibase) thay vì để Hibernate tự động

### 2.4 `jpa.show-sql`

In ra console tất cả câu SQL mà Hibernate thực thi.

```yaml
show-sql: true   # true = bật, false = tắt
```

```sql
-- Ví dụ output:
Hibernate: insert into student (email, first_name, id) values (?, ?, ?)
```

Hữu ích khi debug, theo dõi câu lệnh.

### 2.5 `jpa.properties.hibernate.*`

Các cấu hình nâng cao bên trong `hibernate`.

```yaml
properties:
  hibernate:
    dialect: org.hibernate.dialect.MySQLDialect   # Chỉ định loại database
    format_sql: true                              # Format SQL cho dễ đọc
    use_sql_comments: true                        # Thêm comment giải thích SQL
    generate_statistics: true                     # Thống kê hiệu suất Hibernate
    highlight_sql: true                           # Tô màu SQL trên console
    show_sql: true                                # In SQL ra console
```

| Thuộc tính              | Giá trị | Ý nghĩa                                                                                              |
|-------------------------|---------|-------------------------------------------------------------------------------------------------------|
| `dialect`               | `MySQLDialect` | Chỉ định database để Hibernate sinh SQL đúng cú pháp                         |
| `format_sql`            | `true`  | Format SQL multi-line cho dễ đọc                                                          |
| `use_sql_comments`      | `true`  | Thêm comment vào SQL giải thích Hibernate đang làm gì (ví dụ: `/* load Student */`) |
| `generate_statistics`   | `true`  | Bật thống kê hiệu suất của Hibernate (query count, fetch time...) — chỉ dùng khi debug |
| `highlight_sql`         | `true`  | Tô màu SQL trên console ANSI (cần console hỗ trợ màu)                                     |
| `show_sql`              | `true`  | In SQL ra console (trùng với `jpa.show-sql`)                                                   |

> **Lưu ý:** `show_sql` bên trong `properties.hibernate` trùng lặp với `jpa.show-sql`. Để tránh confuse, chỉ cần dùng `jpa.show-sql` là đủ. Các config kia (`format_sql`, `use_sql_comments`, `highlight_sql`...) chỉ bật khi cần debug chi tiết.

> **Dialect phổ biến:**
> - `MySQLDialect` → MySQL
> - `PostgreSQLDialect` → PostgreSQL
> - `OracleDialect` → Oracle
> - `SQLServerDialect` → SQL Server
>
> Spring Boot 2.x+ có thể **tự nhận diện** dialect từ DataSource, nên dòng `dialect` thường không bắt buộc khai báo.

---

## 3. Entity Annotations

### 3.1 `@Entity`

Đánh dấu class này là một **Entity** — tức ánh xạ đến một bảng trong database.

```java
@Entity
public class JavaUser001 { ... }
```

- Class phải có **constructor không tham số** (Hibernate cần để tạo instance).
- Tên bảng mặc định = tên class (`java_user_001`). Dùng `@Table` để đổi tên.

### 3.2 `@Table`

Chỉ định **tên bảng** trong database ánh xạ với entity này.

```java
@Table(name = "java_user_001")
```

| Thuộc tính | Ý nghĩa |
|------------|---------|
| `name` | Tên bảng trong database |
| `schema` | Tên schema (nếu database có phân chia schema) |
| `catalog` | Tên catalog (dùng cho một số database như MySQL ít dùng) |

> Nếu bỏ `@Table`, Hibernate sẽ dùng tên class làm tên bảng mặc định.

### 3.3 `@Id`

Đánh dấu **Primary Key** — cột định danh duy nhất cho mỗi record.

```java
@Id
private Long id;
```

- Mỗi entity **bắt buộc phải có** đúng **1 trường `@Id`**.
- Kiểu dữ liệu thường dùng: `Long`, `Integer`, `UUID`, `String`.

### 3.4 `@DynamicInsert`

Khi `true`, câu lệnh `INSERT` chỉ chứa **các cột có giá trị** (không phải tất cả cột).

```java
@DynamicInsert
public class JavaUser001 { ... }
```

```sql
-- Bình thường (DynamicInsert = false):
INSERT INTO java_user_001 (id, name, email, phone, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?)

-- Bật DynamicInsert = true (nếu phone, updated_at = null):
INSERT INTO java_user_001 (id, name, email, created_at)
VALUES (?, ?, ?, ?)
```

**Lợi ích:** Giảm kích thước câu SQL, tận dụng giá trị mặc định (`DEFAULT`) của database.

### 3.5 `@DynamicUpdate`

Khi `true`, câu lệnh `UPDATE` chỉ chứa **các cột đã thay đổi** thực sự (dirty columns).

```java
@DynamicUpdate
public class JavaUser001 { ... }
```

```sql
-- Bình thường (DynamicUpdate = false):
UPDATE java_user_001 SET name = ?, email = ?, phone = ?, updated_at = ?
WHERE id = ?

-- Bật DynamicUpdate = true (chỉ name thay đổi):
UPDATE java_user_001 SET name = ? WHERE id = ?
```

**Lợi ích:**
- Giảm kích thước câu SQL
- Tránh ghi đè giá trị mặc định của cột (ví dụ: giá trị `DEFAULT` từ trigger)
- An toàn hơn khi nhiều thread cùng update cùng 1 record

### 3.6 `@GeneratedValue` — chiến lược tạo ID

Khi `@Id` đứng một mình, bạn phải **tự gán giá trị id** trước khi `save()`. `@GeneratedValue` giúp **tự động tạo ID** mà không cần gán thủ công.

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)  // ID tự tăng
private Long id;
```

#### Các chiến lược (`strategy`)

| Chiến lược            | ID tạo ở đâu              | Chịu load? | MySQL tương ứng                  |
|-----------------------|---------------------------|------------|----------------------------------|
| `IDENTITY`            | Database (AUTO_INCREMENT) | Không      | `AUTO_INCREMENT`                 |
| `SEQUENCE`            | Database (sequence object)| Có         | Không hỗ trợ natively            |
| `TABLE`               | Bảng riêng (hibernate_seq)| Không      | Bảng `hibernate_sequences`       |
| `AUTO`                | Hibernate tự chọn         | Tùy DB     | Tùy database                     |
| *(không có strategy)* | Hibernate tự chọn (AUTO)  | Tùy DB     | Tùy database                     |

#### Tại sao `GenerationType.TABLE` sinh ra 2 bảng?

Đây là lý do bạn thấy **2 bảng lạ** xuất hiện khi dùng `@GeneratedValue` mà không chỉ định strategy rõ ràng.

Khi Hibernate chọn chiến lược `TABLE` (hiếm khi, thường xảy ra khi:

- Dùng `@GeneratedValue` mà không chỉ định `strategy` → mặc định là `AUTO`, Hibernate tự quyết
- Database không hỗ trợ SEQUENCE → Hibernate fallback sang TABLE)

Nó tạo **2 bảng system** trong database:

```sql
-- Bảng 1: lưu sequence hiện tại
CREATE TABLE hibernate_sequence (
    next_val BIGINT
);

-- Bảng 2: (nếu có nhiều entity cùng sequence)
CREATE TABLE hibernate_sequences (
    sequence_name VARCHAR(255),
    next_val BIGINT
);
```

Mỗi khi insert, Hibernate:
1. `SELECT next_val FROM hibernate_sequence` → lấy ID
2. `UPDATE hibernate_sequence SET next_val = next_val + 1` → tăng lên 1
3. Dùng ID đó để insert

#### Cách tránh sinh bảng không mong muốn

**Cách 1:** Chỉ định rõ ràng `IDENTITY` cho MySQL (khuyên dùng)

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**Cách 2:** Dùng `SEQUENCE` với MySQL 8+ (nhanh hơn IDENTITY)

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_sequence", allocationSize = 1)
private Long id;
```

```sql
CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 1;
```

#### So sánh IDENTITY vs SEQUENCE vs TABLE

| Tiêu chí         | IDENTITY | SEQUENCE           | TABLE                    |
|------------------|----------|--------------------|--------------------------|
| Nơi sinh ID      | Database | Database           | Bảng riêng trong DB      |
| JDBC batch insert| ❌ Không  | ✅ Có              | ❌ Không                  |
| Hiệu năng        | Trung bình | Rất tốt (batch) | Kém (2 câu SQL/insert)   |
| Hỗ trợ MySQL     | ✅ Có    | ✅ Có (qua proxy)  | ✅ Có                     |
| Sinh bảng phụ    | ❌ Không  | ❌ Không           | ✅ Có (2 bảng)            |

> **Khuyến nghị cho MySQL:** Dùng `GenerationType.IDENTITY` là đơn giản và phổ biến nhất. Nếu cần hiệu năng cao với batch insert, chuyển sang `SEQUENCE`.

### Ví dụ đầy đủ

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity
@Table(name = "java_user_001")
@DynamicInsert
@DynamicUpdate
public class JavaUser001 {

    @Id
    private Long id;
    private String name;
    private String email;
    private String phone;
}
```

> **Lưu ý:** `@Data` của Lombok tự động tạo getter/setter/constructor. Nếu dùng `@Entity`, nên thêm `@NoArgsConstructor` và `@AllArgsConstructor` để Hibernate có thể tạo instance.

---

## 4. Repository — Các cách viết Query

Spring Data JPA cung cấp **4 cách** để viết truy vấn database, từ đơn giản đến phức tạp.

### 4.1 Built-in Methods — CrudRepository / JpaRepository

`JpaRepository` kế thừa từ `CrudRepository`, cung cấp sẵn các method **CRUD cơ bản** — không cần định nghĩa lại.

```java
public interface UserRepository extends JpaRepository<UserEntity, Long> { }
```

#### Các method có sẵn

| Method                    | Kiểu trả về               | Mô tả                               |
|---------------------------|--------------------------|-------------------------------------|
| `save(entity)`            | `S` (entity)             | Insert hoặc Update entity           |
| `findById(id)`            | `Optional<S>`            | Tìm theo ID                         |
| `findAll()`               | `List<S>`                | Lấy tất cả bản ghi                 |
| `findAllById(ids)`        | `List<S>`                | Lấy nhiều bản ghi theo danh sách ID |
| `count()`                 | `long`                   | Đếm tổng số bản ghi                |
| `existsById(id)`          | `boolean`                | Kiểm tra bản ghi có tồn tại không   |
| `deleteById(id)`          | `void`                   | Xóa theo ID                         |
| `delete(entity)`          | `void`                   | Xóa entity                          |
| `deleteAll()`             | `void`                   | Xóa tất cả                          |
| `deleteAllById(ids)`      | `void`                   | Xóa nhiều bản ghi theo danh sách ID |
| `flush()`                 | `void`                   | Flush ngay vào DB ( ép ghi ngay)    |
| `saveAndFlush(entity)`    | `S`                      | Save + flush ngay lập tức          |

#### Phân biệt `save()` — insert hay update?

```java
// INSERT: id = null hoặc chưa tồn tại trong DB
user.setId(null);
userRepository.save(user); // → INSERT

// UPDATE: id đã tồn tại trong DB
user.setName("Hoang PT");
userRepository.save(user); // → UPDATE
```

> **Lưu ý:** `save()` nhận diện insert/update dựa trên giá trị ID và trạng thái entity (managed/detached).

#### JpaRepository vs CrudRepository vs Repository

```
Repository<I, E>
  └── CrudRepository<I, E>
        └── PagingAndSortingRepository<I, E>
              └── JpaRepository<I, E>
```

| Interface                | Methods đặc trưng                                      |
|--------------------------|--------------------------------------------------------|
| `CrudRepository`         | `save`, `findById`, `delete`, `count`, `existsById`   |
| `PagingAndSortingRepository` | `findAll(Pageable)`, `findAll(Sort)`             |
| `JpaRepository`          | `saveAndFlush`, `flush`, `deleteInBatch`, `getById` |

> **Dùng JpaRepository** — là interface đầy đủ nhất, phù hợp cho hầu hết use case.

---

### 4.2 Method Name (Query Method)

Chỉ cần đặt tên method đúng quy tắc, Spring tự sinh SQL.

```java
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Tự động sinh: WHERE user_name = ?
    UserEntity findByUserName(String username);

    // AND nhiều điều kiện
    UserEntity findByUserNameAndEmail(String username, String email);

    // OR
    List<UserEntity> findByUserNameOrEmail(String username, String email);

    // LIKE tìm kiếm
    List<UserEntity> findByUserNameContaining(String keyword);

    // So sánh
    List<UserEntity> findByAgeGreaterThan(int age);
    List<UserEntity> findByAgeBetween(int min, int max);

    // Giới hạn kết quả
    List<UserEntity> findTop3ByUserName(String username);

    // Sắp xếp
    List<UserEntity> findByEmailOrderByCreatedAtDesc(String email);
}
```

**Quy tắc đặt tên:**

```
findBy + Tên field (+ điều kiện)
```

| Từ khóa           | SQL tương ứng                    |
|--------------------|----------------------------------|
| `findByUserName`   | `WHERE user_name = ?`           |
| `findByAgeGreaterThan` | `WHERE age > ?`            |
| `findByNameContaining` | `WHERE name LIKE ?`      |
| `findByNameStartingWith` | `WHERE name LIKE ?%`   |
| `findByNameEndingWith` | `WHERE name LIKE %?`    |
| `findByAgeBetween` | `WHERE age BETWEEN ? AND ?`     |
| `findByEmailIn`    | `WHERE email IN (...)`           |
| `findByIsActive`  | `WHERE is_active = true`        |
| `findByUserNameOrEmail` | `WHERE user_name = ? OR email = ?` |

**Ưu điểm:** Không cần viết SQL, dễ đọc, refactor tự động khi đổi tên field.
**Nhược điểm:** Tên method dài khi query phức tạp, không hỗ trợ JOIN phức tạp.

### 4.2 JPQL (Java Persistence Query Language)

Dùng tên **entity** và **tên field Java** (không phải tên cột DB).

```java
// 1 tham số: dùng ?1 (vị trí)
@Query(value = "SELECT u FROM UserEntity u WHERE u.userName = ?1")
UserEntity findByUserNameJpql(String username);

// Nhiều tham số: dùng ?1, ?2...
@Query(value = "SELECT u FROM UserEntity u WHERE u.userName = ?1 AND u.email = ?2")
UserEntity findByUserNameAndEmail(String username, String email);

// Tham số có tên (rõ ràng hơn)
@Query(value = "SELECT u FROM UserEntity u WHERE u.userName = :username")
UserEntity findByUserNameJpql(@Param("username") String username);
```

> **Phân biệt:**
> - JPQL: dùng tên **entity** (`UserEntity`) và **field Java** (`userName`)
> - Native SQL: dùng tên **bảng** (`user_entity`) và **cột DB** (`user_name`)

**Ưu điểm:** Đọc dễ hơn, Hibernate dịch sang SQL phù hợp với DB đang dùng.
**Nhược điểm:** Cú pháp hơi khác SQL thuần, JOIN phức tạp cần viết tay.

#### Native Query

Viết **SQL thuần** với tên bảng và cột thật trong database.

```java
@Query(value = "SELECT * FROM user_entity WHERE user_name = :username", nativeQuery = true)
UserEntity findByUserNameNative(@Param("username") String username);
```

| Thuộc tính       | Giá trị               | Ý nghĩa                         |
|------------------|-----------------------|---------------------------------|
| `value`          | Câu SQL thuần         | Viết đúng cú pháp MySQL         |
| `nativeQuery`    | `true`                | Bật chế độ SQL thuần            |
| `@Param`         | Tên tham số           | Bind giá trị vào câu SQL        |

**Ưu điểm:** Dùng được tất cả tính năng SQL (stored procedure, CTE, window function...).
**Nhược điểm:** Gắn chặt với database (đổi DB phải sửa lại).

### 4.4 Named Query

Khai báo query trong **entity class** bằng annotation `@NamedQuery`, rồi gọi qua Repository.

```java
// Trong entity UserEntity.java
@Entity
@NamedQuery(
    name = "UserEntity.findByUserName",
    query = "SELECT u FROM UserEntity u WHERE u.userName = :userName"
)
public class UserEntity { ... }
```

```java
// Trong Repository
@Query(name = "UserEntity.findByUserName")
UserEntity findByUserNameNamed(@Param("userName") String username);
```

> **Lưu ý:** `name` trong `@Query` phải khớp chính xác với `name` trong `@NamedQuery` (format: `EntityName.queryName`).

**Ưu điểm:** Query tách biệt khỏi code, dễ quản lý tập trung trong entity.
**Nhược điểm:** Khó debug, không linh hoạt bằng `@Query`.

### 4.5 So sánh 4 cách

| Cách              | Tên entity/field | Gắn DB? | Phức tạp | Dễ debug | Khi nào dùng                         |
|------------------|-------------------|---------|----------|----------|--------------------------------------|
| Method Name      | Java field        | Không   | Thấp     | ✅ Dễ    | Query đơn giản, CRUD cơ bản          |
| JPQL             | Java field        | Không   | Trung bình| ✅ Dễ    | Query trung bình, JOIN đơn giản     |
| Native SQL       | Tên bảng/cột      | ✅ Có    | Cao       | ❌ Khó    | Dùng tính năng đặc thù của DB        |
| Named Query      | Java field        | Không   | Trung bình| ❌ Khó    | Query cố định, tách biệt khỏi code  |

### 4.6 Annotation `@Repository` và `@RepositoryDefinition`

#### `@Repository` — có cần không?

```java
@Repository  // Không bắt buộc
public interface UserRepository extends JpaRepository<UserEntity, Long> { ... }
```

Spring Data JPA **tự động scan** và tạo implementation cho interface extends `JpaRepository`. Annotation `@Repository` trên interface là **thừa** vì:

- `JpaRepository` đã được Spring quản lý
- Không có logic để đánh dấu ở interface
- Exception translation (chuyển SQL exception sang Spring exception) đã được bật mặc định

> **Khi nào cần `@Repository`?** Chỉ cần khi dùng `JdbcTemplate` hoặc tự viết implementation thủ công cho Repository.

#### `@RepositoryDefinition` — thay thế extends

Dùng khi muốn **giới hạn methods** của JpaRepository (không lấy hết).

```java
// Thay vì: extends JpaRepository<UserEntity, Long>
// Dùng @RepositoryDefinition để chỉ lấy các method cần
@RepositoryDefinition(domainClass = UserEntity.class, idClass = Long.class)
public interface UserRepository {
    UserEntity findByUserName(String username);
    UserEntity save(UserEntity entity);
    void deleteById(Long id);
    long count();
}
```

`@RepositoryDefinition` tương đương với việc tự định nghĩa repository, không có sẵn methods như `JpaRepository`.

> **Khi nào dùng?** Khi muốn giới hạn API của repository (ví dụ: không cho delete hàng loạt, không cho truy cập `findAll()`).

### Ví dụ đầy đủ

```java
package com.hoangpt.spring_basic.repository;

import com.hoangpt.spring_basic.entity.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.stereotype.Repository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 1. Method name — Spring tự sinh SQL
    UserEntity findByUserName(String username);

    // 2. Native query — SQL thuần
    @Query(value = "SELECT * FROM user_entity WHERE user_name = ?", nativeQuery = true)
    UserEntity findByUserNameNative(String username);

    // 3. JPQL — dùng tên entity và field Java
    @Query(value = "SELECT u FROM UserEntity u WHERE u.userName = ?")
    UserEntity findByUserNameJpql(String username);

    // 4. Named query — khai báo trong entity
    @Query(name = "UserEntity.findByUserName")
    UserEntity findByUserNameNamed(String username);

### 4.5 Criteria API / Specification

**Criteria API** là cách viết query **bằng code Java thuần**, không cần chuỗi SQL. Spring Data JPA cung cấp `Specification` — một wrapper trên Criteria API — giúp xây dựng query động một cách clean.

#### Cách dùng

Repository cần implement `JpaSpecificationExecutor`:

```java
public interface UserRepository extends
    JpaRepository<UserEntity, Long>,
    JpaSpecificationExecutor<UserEntity> { ... }
```

#### Viết Specification dưới dạng static method

```java
// Cách 1: Viết static method trong Repository
static Specification<UserEntity> hasUserName(String username) {
    return (root, query, cb) -> {
        if (username == null || username.isBlank()) {
            return null; // Trả về null = không có điều kiện
        }
        return cb.equal(root.get("userName"), username);
    };
}

static Specification<UserEntity> hasEmail(String email) {
    return (root, query, cb) -> cb.equal(root.get("email"), email);
}

static Specification<UserEntity> hasAgeGreaterThan(int age) {
    return (root, query, cb) -> cb.greaterThan(root.get("age"), age);
}
```

#### Cách gọi trong Service

```java
// Tìm theo username
Optional<UserEntity> user = userRepository.findOne(hasUserName("hoangpt"));

// Kết hợp nhiều điều kiện: username AND email
Specification<UserEntity> spec = hasUserName("hoangpt").and(hasEmail("hoang@example.com"));
Optional<UserEntity> user = userRepository.findOne(spec);

// Hoặc OR
Specification<UserEntity> spec = hasUserName("hoangpt").or(hasEmail("hoang@example.com"));

// Nhiều điều kiện động (query động)
Specification<UserEntity> spec = Specification.where(null);
if (username != null) spec = spec.and(hasUserName(username));
if (email != null)     spec = spec.and(hasEmail(email));
List<UserEntity> users = userRepository.findAll(spec);
```

#### Các method có sẵn trong `JpaSpecificationExecutor`

```java
// Tìm 1 bản ghi
Optional<UserEntity> findOne(Specification<UserEntity> spec);

// Tìm tất cả (phân trang)
Page<UserEntity> findAll(Specification<UserEntity> spec, Pageable pageable);

// Tìm tất cả (sắp xếp)
List<UserEntity> findAll(Specification<UserEntity> spec, Sort sort);

// Đếm
long count(Specification<UserEntity> spec);

// Kiểm tra tồn tại
boolean exists(Specification<UserEntity> spec);
```

#### Các toán tử Criteria hay dùng

| Toán tử                | Criteria API              | SQL tương ứng                |
|------------------------|---------------------------|------------------------------|
| Bằng                   | `cb.equal(root.get("x"), val)` | `x = ?`                |
| Không bằng             | `cb.notEqual(root.get("x"), val)` | `x <> ?`           |
| Lớn hơn                | `cb.greaterThan(root.get("x"), val)` | `x > ?`       |
| Nhỏ hơn                | `cb.lessThan(root.get("x"), val)` | `x < ?`          |
| Lớn hơn hoặc bằng      | `cb.greaterThanOrEqualTo(root.get("x"), val)` | `x >= ?` |
| LIKE                   | `cb.like(root.get("x"), "%" + val + "%")` | `x LIKE ?` |
| Null                   | `cb.isNull(root.get("x"))` | `x IS NULL`              |
| Not null               | `cb.isNotNull(root.get("x"))` | `x IS NOT NULL`       |
| IN                     | `cb.in(root.get("x")).value(list)` | `x IN (...)`      |
| AND                    | `spec1.and(spec2)`        | `AND`                       |
| OR                     | `spec1.or(spec2)`         | `OR`                        |

> **Ưu điểm:** An toàn kiểu (compile-time check), query động dễ dàng, không chuỗi SQL trong code.
> **Nhược điểm:** Cú pháp verbose, cần hiểu rõ `root`, `query`, `cb`.

---

### 4.6 QueryDSL

**QueryDSL** sinh **các class Q** tự động từ entity tại compile time, cho phép viết query với **syntax chain rất giống SQL**.

#### Cài đặt

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-apt</artifactId>
    <scope>provided</scope>
</dependency>
```

#### Cách dùng

Repository cần implement `QuerydslPredicateExecutor`:

```java
public interface UserRepository extends
    JpaRepository<UserEntity, Long>,
    QuerydslPredicateExecutor<UserEntity> { ... }
```

#### Class Q tự sinh

```java
// Sau khi build, class Q sẽ được tạo:
// QUserEntity.userEntity — tương ứng với UserEntity

import static com.hoangpt.spring_basic.entity.user.QUserEntity.userEntity;
```

#### Viết query với QueryDSL

```java
// Tìm theo username
UserEntity user = repository.findOne(userEntity.userName.eq("hoangpt"));

// Nhiều điều kiện
UserEntity user = repository.findOne(
    userEntity.userName.eq("hoangpt")
    .and(userEntity.email.eq("hoang@example.com"))
);

// LIKE
List<UserEntity> users = repository.findAll(
    userEntity.userName.contains("hoang")
);

// IN
List<UserEntity> users = repository.findAll(
    userEntity.id.in(Arrays.asList(1L, 2L, 3L))
);

// Sắp xếp và giới hạn
List<UserEntity> users = repository.findAll(
    userEntity.email.startsWith("admin")
    .and(userEntity.isActive.eq(true)),
    userEntity.createdAt.desc()
);
```

#### So sánh Criteria API (Specification) vs QueryDSL

| Tiêu chí           | Criteria API / Specification | QueryDSL                 |
|--------------------|------------------------------|--------------------------|
| Cú pháp            | Phương thức (`cb.equal(...)`) | Chain (`x.eq(...)`)  |
| Kiểu an toàn       | ✅ Tốt                       | ✅ Rất tốt               |
| Dependencies       | Không cần thêm               | Cần thêm library + APT   |
| Sinh Q-class       | Không cần                    | ✅ Cần (chạy annotation processor) |
| Độ phức tạp        | Cao                          | Trung bình               |
| Hỗ trợ JOIN        | Phức tạp                     | Dễ hơn                   |
| Phổ biến           | Phổ biến trong enterprise    | Ít phổ biến hơn          |

> **Khuyến nghị:** Dùng **Specification** khi cần query động (lọc theo nhiều điều kiện tùy chọn). Dùng **QueryDSL** khi muốn syntax chain gần với SQL, hoặc cần JOIN phức tạp.

---

### 4.7 `@Modifying` và `@Transactional`

Hai annotation này **bắt buộc** khi dùng `@Query` để **UPDATE** hoặc **DELETE**.

#### `@Modifying`

Báo cho Spring Data JPA biết query này **thay đổi dữ liệu** (INSERT/UPDATE/DELETE), không phải SELECT.

```java
@Modifying
@Query("UPDATE UserEntity u SET u.isActive = false WHERE u.email = :email")
int deactivateByEmail(@Param("email") String email);
```

> Nếu không có `@Modifying`, Spring sẽ ném `IllegalStateException: Query ilike UPDATE/DELETE must be annotated with @Modifying`.

#### `@Transactional`

Bao bọc toàn bộ thao tác trong **1 transaction**. Nếu không có:

- Query sẽ **không được commit** → dữ liệu không lưu vào DB
- Nhiều câu SQL không được nhóm lại → hiệu năng kém

```java
@Modifying
@Transactional  // Bắt buộc khi @Query thay đổi dữ liệu
@Query("UPDATE UserEntity u SET u.isActive = false WHERE u.email = :email")
int deactivateByEmail(@Param("email") String email);
```

#### Nên đặt ở Repository hay Service?

| Vị trí đặt           | Phạm vi transaction                  | Dùng khi                              |
|----------------------|--------------------------------------|---------------------------------------|
| Trên **Repository** method | Chỉ câu SQL đó được transaction | Query đơn lẻ, không có logic phức tạp |
| Trên **Service** method | **Tất cả** câu SQL trong method cùng 1 transaction | Cần nhiều bước, nhiều bảng           |

**Ví dụ thực tế — đặt trên Repository có vấn đề gì?**

```java
// ❌ Repository — mỗi call tự có transaction riêng
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @Modifying
    @Transactional
    void deactivateUser(Long userId);

    @Modifying
    @Transactional
    void sendWelcomeEmail(Long userId);
}
```

```java
// ❌ Service gọi 2 method
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public void registerUser(Long userId) {
        userRepository.deactivateUser(userId);     // Transaction 1
        userRepository.sendWelcomeEmail(userId);   // Transaction 2 — tách biệt!
        // Nếu sendWelcomeEmail lỗi → deactivateUser vẫn đã commit rồi!
        // Không rollback được → data không nhất quán
    }
}
```

```java
// ✅ Đặt trên Service — cả 2 bước cùng 1 transaction
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void registerUser(Long userId) {
        userRepository.deactivateUser(userId);     // Cùng transaction
        userRepository.sendWelcomeEmail(userId);   // Cùng transaction
        // Nếu bước 2 lỗi → cả 2 đều rollback
    }
}
```

**Tóm tại sao Service:**

1. **Atomic (tính nguyên tử):** Nhiều bước → cùng rollback hoặc cùng commit
2. **Tách biệt layer:** Repository chỉ lo truy vấn, Service lo nghiệp vụ
3. **Dễ mở rộng:** Thêm bước mới vào Service vẫn đảm bảo transaction
4. **Mặc định Spring:** Nếu không đặt, Spring dùng `TransactionPropagation.REQUIRED` — tìm transaction hiện có, không có thì tạo mới

> **Ngoại lệ:** Đặt `@Transactional` trên **Repository** chỉ khi query đó đứng riêng lẻ, không gọi chung với bước nào khác.

#### Các thuộc tính thường dùng

```java
@Transactional(
    propagation = Propagation.REQUIRED,    // Mặc định — dùng transaction hiện có hoặc tạo mới
    isolation = Isolation.READ_COMMITTED, // Mức cô lập — tránh dirty read
    readOnly = false,                    // true = chỉ đọc, tối ưu hiệu năng
    rollbackFor = Exception.class,        // Rollback khi có exception (mặc định chỉ rollback RuntimeException)
    timeout = 30                          // Timeout 30 giây
)
```

| Thuộc tính       | Giá trị phổ biến           | Ý nghĩa                                 |
|-----------------|---------------------------|------------------------------------------|
| `propagation`   | `REQUIRED` (mặc định)      | Dùng transaction hiện có, không có thì tạo mới |
| `readOnly`      | `true`                     | Tối ưu đọc, Hibernate bỏ qua dirty check |
| `rollbackFor`   | `Exception.class`          | Rollback khi gặp Exception (mặc định chỉ rollback RuntimeException) |
| `timeout`       | `30` (giây)                | Hủy nếu transaction kéo dài quá lâu    |

```java
// Ví dụ: đọc data, không thay đổi — dùng readOnly
@Transactional(readOnly = true)
public List<UserEntity> getAllActiveUsers() {
    return userRepository.findAll();
}

// Ví dụ: rollback kể cả checked exception
@Transactional(rollbackFor = Exception.class)
public void processUser(Long userId) throws Exception {
    userRepository.updateStatus(userId);
    sendEmail(userId);
}
```

#### Cách đặt

```java
// Cách 1: Đặt @Transactional trên Repository method
@Modifying
@Transactional
@Query("DELETE FROM UserEntity u WHERE u.isActive = false")
int deleteInactiveUsers();

// Cách 2: Đặt @Transactional trên Service (khuyên dùng)
@Service
public class UserService {

    @Transactional
    public void deactivateUser(String email) {
        repository.deactivateByEmail(email);
    }
}
```

#### `@Modifying(clearAutomatically = true)`

Mặc định sau khi `@Modifying` chạy, Hibernate **cache vẫn giữ data cũ**. `clearAutomatically = true` xóa cache ngay để các query sau nhận data mới.

```java
@Modifying(clearAutomatically = true)
@Query("UPDATE UserEntity u SET u.name = :name WHERE u.id = :id")
void updateName(@Param("id") Long id, @Param("name") String name);
```

#### Tóm tắt nhanh

| Annotation              | Bắt buộc? | Mục đích                               |
|------------------------|-----------|----------------------------------------|
| `@Modifying`           | ✅ Có      | Báo đây là query UPDATE/DELETE        |
| `@Transactional`       | ✅ Có      | Đảm bảo commit, rollback nếu lỗi     |
| `@Modifying(clearAutomatically)` | Không | Xóa Hibernate cache sau query        |
| `readOnly = true`      | Không     | Tối ưu hiệu năng khi chỉ đọc        |
| `rollbackFor = ...`    | Không     | Rollback với cả checked exception     |

---

### Ví dụ đầy đủ

```java
public interface UserRepository extends
    JpaRepository<UserEntity, Long>,
    JpaSpecificationExecutor<UserEntity> {

    // 1. Method name
    UserEntity findByUserName(String username);

    // 2. Native query
    @Query(value = "SELECT * FROM user_entity WHERE user_name = ?", nativeQuery = true)
    UserEntity findByUserNameNative(String username);

    // 3. JPQL
    @Query(value = "SELECT u FROM UserEntity u WHERE u.userName = ?")
    UserEntity findByUserNameJpql(String username);

    // 4. Named query
    @Query(name = "UserEntity.findByUserName")
    UserEntity findByUserNameNamed(String username);

    // 5. Specification — dùng trong Service
    static Specification<UserEntity> hasUserName(String username) {
        return (root, query, cb) ->
            username == null ? null : cb.equal(root.get("userName"), username);
    }

    // 6. QueryDSL — cần cấu hình thêm
    // repository.findAll(QUserEntity.userEntity.userName.eq(username));
}
```

---

## 5. Quan hệ Entity — Cascade, JoinColumn, Relationships

### 5.1 Cascade — Khi nào thao tác được lan truyền?

**Cascade** quyết định khi thực hiện thao tác (persist, merge, remove, refresh, detach) trên entity cha thì entity con có bị ảnh hưởng không.

```java
@ManyToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "userId", nullable = false)
private UserEntity user;
```

#### Các loại CascadeType

| CascadeType       | Thao tác lan truyền                                         | Ví dụ thực tế                                     |
|-------------------|------------------------------------------------------------|----------------------------------------------------|
| `ALL`             | Tất cả các loại bên dưới                                    | Cha có gì, con đều theo                           |
| `PERSIST`         | Khi `save()` cha → con cũng được `save()` tự động           | Tạo Order → OrderItem tự tạo theo                  |
| `MERGE`           | Khi `merge()` cha → con cũng được `merge()`                | Cập nhật Order → OrderItem liên quan được merge   |
| `REMOVE`          | Khi xóa cha → con cũng bị xóa                               | Xóa User → Comment của user cũng bị xóa          |
| `REFRESH`         | Khi `refresh()` cha → con cũng được `refresh()`            | Làm mới parent → làm mới child từ DB              |
| `DETACH`          | Khi `detach()` cha → con cũng bị `detach()`                | Tách cha khỏi persistence context → con cũng tách |

```java
// Ví dụ: CascadeType.ALL — tất cả thao tác lan truyền
@OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
private List<CommentEntity> comments;
```

```java
// Ví dụ: chỉ định rõ — vừa tạo vừa xóa theo, không cascade update
@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "user")
private List<CommentEntity> comments;
```

#### Ví dụ thực tế — Order và OrderItem

```java
// Entity Cha: Order
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double total;

    // Khi save Order → OrderItem tự save
    // Khi xóa Order → OrderItem tự xóa
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order")
    private List<OrderItem> items = new ArrayList<>();
}
```

```java
// Entity Con: OrderItem
@Entity
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
```

```java
// Service — chỉ cần save cha, con tự theo
@Transactional
public void createOrder() {
    Order order = new Order();
    order.setTotal(100.0);

    OrderItem item = new OrderItem();
    item.setOrder(order);           // set cả 2 chiều!
    order.getItems().add(item);

    orderRepository.save(order);    // Chỉ cần save cha
    // item được persist tự động nhờ CascadeType.ALL
}
```

> ⚠️ **Cẩn thận với `CascadeType.REMOVE`:** Xóa cha → xóa hết con. Không dùng trên production nếu chưa có strategy backup. Dùng thêm `orphanRemoval = true` để xóa con khi bị remove khỏi collection.

#### So sánh nhanh

| CascadeType       | Tạo con theo cha | Xóa con theo cha | Merge con theo cha |
|-------------------|-----------------|------------------|--------------------|
| `ALL`             | ✅              | ✅               | ✅                  |
| `PERSIST`         | ✅              | ❌               | ❌                  |
| `REMOVE`          | ❌              | ✅               | ❌                  |
| `MERGE`           | ❌              | ❌               | ✅                  |

### 5.2 `@JoinColumn` — chỉ định cột khóa ngoại

`@JoinColumn` chỉ định **cột FK** trong bảng hiện tại tham chiếu đến PK của bảng kia.

```java
@ManyToOne(cascade = CascadeType.ALL, optional = false)
@JoinColumn(name = "userId", nullable = false)
private UserEntity user;
```

| Thuộc tính               | Giá trị              | Ý nghĩa                                         |
|--------------------------|----------------------|-------------------------------------------------|
| `name`                   | `"userId"`           | Tên cột FK trong bảng hiện tại                 |
| `referencedColumnName`   | `"id"` (mặc định)   | Tên cột PK ở bảng kia                           |
| `nullable`               | `false`              | FK không được NULL                              |
| `unique`                 | `false`              | FK có phải unique không (dùng cho quan hệ 1-1)  |
| `insertable`              | `true`               | Có cho phép insert vào cột này không           |
| `updatable`               | `true`               | Có cho phép update vào cột này không           |
| `columnDefinition`        | —                    | Ghi đè kiểu cột SQL (tùy chỉnh)                |

> **Phân biệt:** `@Column` chỉ định cột thường trong bảng hiện tại. `@JoinColumn` chỉ định cột FK tham chiếu bảng khác.

### 5.3 `@ManyToOne` — quan hệ Nhiều-Một

**Nghĩa:** Nhiều record ở bảng hiện tại tham chiếu đến **1 record** ở bảng kia.

```
Comment_table (nhiều comment)
  └── FK: user_id ────→ user_entity (1 user viết nhiều comment)
```

| Annotation    | Ý nghĩa                                                    |
|---------------|------------------------------------------------------------|
| `@ManyToOne` | Nhiều-đối-một → Comment nhiều, User một                  |
| `@OneToMany` | Một-đối-nhiều → User một, List<Comment> nhiều           |
| `@OneToOne`  | Một-đối-một                                               |
| `@ManyToMany` | Nhiều-đối-nhiều                                            |

#### Thuộc tính `optional`

| Giá trị             | Ý nghĩa                                      | Ràng buộc DB          |
|---------------------|----------------------------------------------|-----------------------|
| `optional = true` (mặc định) | FK có thể NULL — comment có thể không có user | `user_id NULL`     |
| `optional = false`  | FK bắt buộc NOT NULL — mỗi comment phải có user | `user_id NOT NULL` |

```java
// optional = false: comment bắt buộc phải có user
@ManyToOne(optional = false)
@JoinColumn(name = "userId", nullable = false)
private UserEntity user;
```

```sql
-- Hibernate sinh ra:
-- user_id BIGINT NOT NULL,
-- FOREIGN KEY (user_id) REFERENCES user_entity(id)
```

### 5.4 Quan hệ 2 chiều và `mappedBy`

#### Cách hoạt động

```java
// ===== Bên sở hữu FK (Owner side) — có JoinColumn = tạo cột FK =====
@ManyToOne
@JoinColumn(name = "userId")
private UserEntity user;

// ===== Bên không sở hữu FK (Inverse side) — dùng mappedBy, KHÔNG tạo cột FK =====
// mappedBy = tên field bên kia tham chiếu đến mình
@OneToMany(mappedBy = "user")
private List<CommentEntity> comments;
```

| Bên             | Có `@JoinColumn`? | Có `mappedBy`? | Tạo cột FK? | Mục đích              |
|-----------------|-------------------|---------------|------------|------------------------|
| **Owner side**  | ✅ Có              | ❌ Không       | ✅ Có       | Sở hữu FK thật sự     |
| **Inverse side**| ❌ Không           | ✅ Có          | ❌ Không    | Chỉ ngầm liên kết     |

> **`mappedBy` không bao giờ tạo cột FK trong database.** FK luôn nằm ở bên có `JoinColumn` (Owner side).

### 5.4 `orphanRemoval` — xóa con khi bị gỡ khỏi cha

`orphanRemoval` là thuộc tính bổ sung cho `@OneToMany`, có nghĩa: **con bị xóa khỏi database khi bị remove khỏi collection của cha**.

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<CommentEntity> comments = new ArrayList<>();
```

#### So sánh `CascadeType.REMOVE` vs `orphanRemoval`

| Tiêu chí         | `CascadeType.REMOVE`                        | `orphanRemoval = true`                              |
|------------------|---------------------------------------------|----------------------------------------------------|
| Con bị xóa khi    | Cha bị xóa                                  | Cha bị xóa **HOẶC** con bị `remove()` khỏi list  |
| Bắt buộc cha?     | Không bắt buộc — con vẫn tồn tại khi cha xóa | Không bắt buộc                                    |
| Trigger          | `entityRepository.delete(cha)`               | `cha.getComments().remove(comment)`                 |

```java
// Ví dụ: orphanRemoval = true
@Transactional
public void removeComment() {
    UserEntity user = userRepository.findById(1L);

    CommentEntity comment = user.getComments().get(0);
    user.getComments().remove(comment);   // Xóa khỏi list

    userRepository.save(user);
    // comment bị xóa khỏi DB tự động nhờ orphanRemoval = true
}
```

```java
// Ví dụ: CascadeType.REMOVE (không có orphanRemoval)
@Transactional
public void removeComment() {
    UserEntity user = userRepository.findById(1L);

    user.getComments().remove(0);          // Chỉ xóa khỏi list
    userRepository.save(user);
    // ❌ Comment vẫn còn trong DB — không bị xóa
}
```

#### Khi nào dùng?

| Tình huống                          | Cấu hình                               |
|-------------------------------------|----------------------------------------|
| Xóa cha → xóa hết con               | `cascade = CascadeType.ALL`           |
| Xóa cha → xóa hết con (không cascade ALL) | `cascade = CascadeType.REMOVE`      |
| Xóa khỏi list → xóa con            | `orphanRemoval = true`                |
| **Combo đầy đủ:**                   | `cascade = CascadeType.ALL, orphanRemoval = true` |

> **Lưu ý:** `orphanRemoval = true` tự động ngầm kéo theo `CascadeType.REMOVE`. Tuy nhiên `CascadeType.REMOVE` **không** ngầm kéo theo `orphanRemoval`.

#### Tóm tắt: `@OneToMany` đầy đủ

```java
@OneToMany(
    mappedBy = "user",           // Inverse side — không tạo FK
    cascade = CascadeType.ALL,   // Tất cả thao tác trên cha lan sang con
    orphanRemoval = true         // Xóa con khi bị remove khỏi list
)
private List<CommentEntity> comments = new ArrayList<>();
```

---

#### Phải set cả 2 chiều khi thêm vào collection

```java
@Transactional
public void addComment() {
    CommentEntity comment = new CommentEntity();
    comment.setUser(user);                  // Bên owner — set entity
    user.getComments().add(comment);         // Bên inverse — thêm vào list
    commentRepository.save(comment);        // Chỉ cần save comment
}
```

### 5.5 Bảng tổng hợp quan hệ Entity

| Quan hệ        | Cột FK ở đâu? | mappedBy? | Ví dụ                      |
|---------------|---------------|-----------|----------------------------|
| `@ManyToOne` | Bảng con      | Không     | Comment → User            |
| `@OneToMany` | Bảng con      | ✅ Có      | User → List<Comment>       |
| `@OneToOne`  | 1 trong 2 bên| Có        | User → UserProfile         |
| `@ManyToMany`| Bảng trung gian| Có       | Student ↔ Course           |

### 5.6 Ví dụ đầy đủ

```java
// ===== CommentEntity.java (Owner side) =====
@Entity
@Table(name = "comment")
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    // Owner side — sở hữu FK, bắt buộc có user, cascade ALL
    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private UserEntity user;
}

// ===== UserEntity.java (Inverse side) =====
@Entity
@Table(name = "user_entity")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;

    // Inverse side — không sở hữu FK, cascade theo owner
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<CommentEntity> comments = new ArrayList<>();
}
```

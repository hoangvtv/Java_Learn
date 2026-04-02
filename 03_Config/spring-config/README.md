# Spring Boot Configuration File Priority

## Mục lục

- [Thứ tự ưu tiên cấu hình](#thứ-tự-ưu-tiên-cấu-hình)
- [Nguyên tắc áp dụng](#nguyên-tắc-áp-dụng)
- [Ví dụ thực tế](#ví-dụ-thực-tế)
- [spring.profiles.active](#springprofilesactive)
  - [Cách chạy với profile cụ thể](#cách-chạy-với-profile-cụ-thể)
  - [Nguyên tắc ghi đè](#nguyên-tắc-ghi-đè)
- [Tham khảo](#tham-khảo)

## Thứ tự ưu tiên cấu hình

Thứ tự ưu tiên các file cấu hình trong Spring Boot (từ **cao nhất** đến **thấp nhất**):

| Priority | Nguồn cấu hình | Mô tả |
|:--------:|----------------|-------|
| 1 | **Command Line Arguments** (`--spring.config.location=...`) | Tham số truyền qua dòng lệnh khi chạy ứng dụng |
| 2 | **SPRING_APPLICATION_JSON** | Biến môi trường chứa JSON (`SPRING_APPLICATION_JSON`) |
| 3 | **JNDI** | Thuộc tính từ Java Naming and Directory Interface |
| 4 | **OS Environment Variables** | Biến môi trường hệ điều hành |
| 5 | **RandomValuePropertySource** | Giá trị sinh ngẫu nhiên (`${random.int}`, `${random.uuid}`, ...) |
| 6 | **Jar外特定配置文件** (`classpath:/config/`) | File cấu hình profile cụ thể bên ngoài JAR, đặt trong thư mục `/config` |
| 7 | **Jar外默认配置文件** (`classpath:/`) | File cấu hình mặc định bên ngoài JAR, đặt ở root classpath |
| 8 | **Jar内特定配置文件** (`application-{profile}.properties`) | File cấu hình cho profile cụ thể bên trong JAR (e.g. `application-dev.properties`) |
| 9 | **Jar内默认配置文件** (`application.properties`) | File cấu hình mặc định bên trong JAR |
| 10 | **@PropertySource** | Annotation trên class `@Configuration` để load file `.properties` |
| 11 | **SpringApplication.setDefaultProperties** | Cấu hình mặc định đặt trong code (programmatic) |

## Nguyên tắc áp dụng

- **Cấu hình sau ghi đè cấu hình trước** — cùng một property, giá trị ở dòng cao hơn (số thấp hơn) sẽ được ưu tiên.
- **Profile-specific** luôn ghi đè **default** — e.g. `application-dev.properties` ghi đè `application.properties`.
- **Bên ngoài JAR** (filesystem) luôn ghi đè **bên trong JAR** (classpath) — cho phép override mà không cần repackage.

## Ví dụ thực tế

```bash
# Ưu tiên cao nhất: command line argument
java -jar app.jar --spring.datasource.url=jdbc:mysql://prod:3306/mydb
```

```properties
# SPRING_APPLICATION_JSON (ưu tiên 2)
# Set qua biến môi trường:
export SPRING_APPLICATION_JSON='{"spring":{"datasource":{"url":"jdbc:mysql://prod:3306/mydb"}}}'
```

## spring.profiles.active

Khai báo profile đang active trong `application.yaml`:

```yaml
spring:
  profiles:
    active: test   # active profile là "test"
```

Các profile có sẵn trong project:

| Profile | File cấu hình | Port | Mô tả |
|---------|--------------|------|-------|
| `dev` | `application-dev.yaml` | 8082 | Môi trường phát triển |
| `test` | `application-test.yaml` | 8083 | Môi trường kiểm thử |
| `prod` | `application-prod.yaml` | 8084 | Môi trường production |

### Cách chạy với profile cụ thể

#### 1. Qua command line argument (ưu tiên cao nhất)

```bash
java -jar app.jar --spring.profiles.active=prod
```

Trong **IntelliJ IDEA**: Vào `Run > Edit Configurations > Build and Run`, thêm vào phần **Program arguments**:

```bash
--spring.profiles.active=prod
```

#### 2. Qua environment variable

```bash
export SPRING_PROFILES_ACTIVE=prod
```

Trong **IntelliJ IDEA**: Vào `Run > Edit Configurations > Build and Run`, thêm vào phần **Environment variables**:

```bash
SPRING_PROFILES_ACTIVE=prod
```

#### 3. Qua VM options

Trong **IntelliJ IDEA**: Vào `Run > Edit Configurations > Build and Run`, thêm vào phần **VM options**:

```bash
-Dspring.profiles.active=prod
```

#### Tóm tắt trong IntelliJ IDEA

| Phương thức | Vị trí trong Run Configuration |
|-------------|-------------------------------|
| Program arguments | `--spring.profiles.active=prod` |
| Environment variables | `SPRING_PROFILES_ACTIVE=prod` |
| VM options | `-Dspring.profiles.active=prod` |

### Nguyên tắc ghi đè

- Cùng một property trong `application-{profile}.yaml` sẽ **ghi đè** giá trị trong `application.yaml`.
- Ví dụ: `profile.name` trong `application-dev.yaml` ghi đè `profile.name` trong `application.yaml`.

## Tham khảo

![Spring Boot Configuration File Priority](src/main/resources/static/assets/priority-config-file.png)

# Hướng Dẫn Kết Nối Database Bằng Docker

## Mục Lục
1. [Yêu Cầu Hệ Thống](#yêu-cầu-hệ-thống)
2. [Cài Đặt Docker](#cài-đặt-docker)
3. [Khởi Chạy PostgreSQL Container](#khởi-chạy-postgresql-container)
4. [Cấu Hình Ứng Dụng](#cấu-hình-ứng-dụng)
5. [Quản Lý Database](#quản-lý-database)
6. [Xử Lý Sự Cố](#xử-lý-sự-cố)

---

## Yêu Cầu Hệ Thống

- **Docker Desktop** (Windows/Mac) hoặc **Docker Engine** (Linux)
- **Java 25** trở lên
- **Maven** 3.6+
- Ít nhất **2GB RAM** trống cho container

---

## Cài Đặt Docker

### Windows
1. Tải Docker Desktop từ: https://www.docker.com/products/docker-desktop
2. Chạy file cài đặt và làm theo hướng dẫn
3. Khởi động lại máy tính nếu được yêu cầu
4. Mở PowerShell và kiểm tra:
   ```powershell
   docker --version
   docker-compose --version
   ```

### macOS
1. Tải Docker Desktop từ: https://www.docker.com/products/docker-desktop
2. Kéo Docker.app vào thư mục Applications
3. Mở Docker từ Applications
4. Kiểm tra trong Terminal:
   ```bash
   docker --version
   docker-compose --version
   ```

### Linux (Ubuntu/Debian)
```bash
# Cài đặt Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Thêm user vào docker group
sudo usermod -aG docker $USER

# Khởi động Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Kiểm tra
docker --version
```

---

## Khởi Chạy PostgreSQL Container

### Phương Pháp 1: Sử Dụng Docker Command (Đơn Giản)

#### Khởi chạy PostgreSQL container:
```bash
docker run -d \
  --name demo_web_postgres \
  -e POSTGRES_DB=demo_web_db \
  -e POSTGRES_USER=demo_user \
  -e POSTGRES_PASSWORD=demo_password \
  -p 5432:5432 \
  -v demo_web_data:/var/lib/postgresql/data \
  postgres:15-alpine
```

**Giải thích các tham số:**
- `--name demo_web_postgres`: Tên container
- `-e POSTGRES_DB`: Tên database
- `-e POSTGRES_USER`: Username
- `-e POSTGRES_PASSWORD`: Password
- `-p 5432:5432`: Map port 5432 từ container ra host
- `-v demo_web_data:/var/lib/postgresql/data`: Lưu trữ dữ liệu persistent
- `postgres:15-alpine`: Image PostgreSQL phiên bản 15 (nhẹ)

#### Kiểm tra container đang chạy:
```bash
docker ps
```

#### Xem logs:
```bash
docker logs demo_web_postgres
```

---

### Phương Pháp 2: Sử Dụng Docker Compose (Khuyến Nghị)

#### Tạo file `docker-compose.yml` trong thư mục gốc project:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: demo_web_postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: demo_web_db
      POSTGRES_USER: demo_user
      POSTGRES_PASSWORD: demo_password
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U demo_user -d demo_web_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Optional: pgAdmin for database management
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: demo_web_pgadmin
    restart: unless-stopped
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@demo.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

volumes:
  postgres_data:
    driver: local
```

#### Khởi chạy:
```bash
# Khởi động tất cả services
docker-compose up -d

# Xem logs
docker-compose logs -f

# Dừng services
docker-compose down

# Dừng và xóa volumes (XÓA DỮ LIỆU!)
docker-compose down -v
```

---

## Cấu Hình Ứng Dụng

### 1. Tạo file `application-dev.properties`

Tạo file `src/main/resources/application-dev.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/demo_web_db
spring.datasource.username=demo_user
spring.datasource.password=demo_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=1200000

# Logging
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### 2. Tạo file `application-prod.properties`

Tạo file `src/main/resources/application-prod.properties`:

```properties
# Production Database Configuration
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:demo_web_db}
spring.datasource.username=${DB_USER:demo_user}
spring.datasource.password=${DB_PASSWORD:demo_password}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Logging
logging.level.org.hibernate.SQL=WARN
```

### 3. Cập nhật `.gitignore`

Đảm bảo file `.gitignore` chứa:
```
# Sensitive configuration files
application-dev.properties
application-prod.properties
```

---

## Quản Lý Database

### Kết Nối Trực Tiếp Vào PostgreSQL Container

```bash
# Truy cập PostgreSQL CLI
docker exec -it demo_web_postgres psql -U demo_user -d demo_web_db

# Hoặc sử dụng bash
docker exec -it demo_web_postgres bash
psql -U demo_user -d demo_web_db
```

### Các Lệnh PostgreSQL Hữu Ích

```sql
-- Liệt kê tất cả databases
\l

-- Kết nối đến database
\c demo_web_db

-- Liệt kê tất cả tables
\dt

-- Xem cấu trúc table
\d table_name

-- Xem tất cả users
\du

-- Thoát
\q
```

### Sử Dụng pgAdmin (Nếu đã cài)

1. Mở trình duyệt: http://localhost:5050
2. Đăng nhập:
   - Email: `admin@demo.com`
   - Password: `admin`
3. Thêm server mới:
   - **Name**: Demo Web DB
   - **Host**: `postgres` (nếu dùng docker-compose) hoặc `host.docker.internal` (Windows/Mac)
   - **Port**: `5432`
   - **Database**: `demo_web_db`
   - **Username**: `demo_user`
   - **Password**: `demo_password`

### Backup và Restore

#### Backup Database
```bash
# Backup toàn bộ database
docker exec demo_web_postgres pg_dump -U demo_user demo_web_db > backup.sql

# Backup với format custom (nén)
docker exec demo_web_postgres pg_dump -U demo_user -Fc demo_web_db > backup.dump
```

#### Restore Database
```bash
# Restore từ SQL file
docker exec -i demo_web_postgres psql -U demo_user -d demo_web_db < backup.sql

# Restore từ custom format
docker exec -i demo_web_postgres pg_restore -U demo_user -d demo_web_db < backup.dump
```

---

## Xử Lý Sự Cố

### 1. Container không khởi động được

**Kiểm tra logs:**
```bash
docker logs demo_web_postgres
```

**Kiểm tra port đã bị chiếm:**
```bash
# Windows
netstat -ano | findstr :5432

# Linux/Mac
lsof -i :5432
```

**Giải pháp:** Thay đổi port mapping trong docker command hoặc docker-compose.yml:
```yaml
ports:
  - "5433:5432"  # Sử dụng port 5433 thay vì 5432
```

### 2. Không kết nối được từ ứng dụng

**Kiểm tra container đang chạy:**
```bash
docker ps | grep postgres
```

**Kiểm tra network:**
```bash
docker inspect demo_web_postgres | grep IPAddress
```

**Kiểm tra credentials trong application-dev.properties**

### 3. Lỗi "password authentication failed"

**Reset password:**
```bash
docker exec -it demo_web_postgres psql -U postgres
ALTER USER demo_user WITH PASSWORD 'new_password';
\q
```

### 4. Dữ liệu bị mất sau khi restart

**Đảm bảo sử dụng volume:**
```bash
# Kiểm tra volumes
docker volume ls

# Kiểm tra volume được mount
docker inspect demo_web_postgres | grep Mounts -A 10
```

### 5. Container chạy chậm

**Tăng resources cho Docker Desktop:**
- Settings → Resources → Advanced
- Tăng CPU và Memory

**Tối ưu connection pool trong application.properties:**
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

---

## Các Lệnh Docker Hữu Ích

```bash
# Xem tất cả containers (bao gồm stopped)
docker ps -a

# Khởi động container đã dừng
docker start demo_web_postgres

# Dừng container
docker stop demo_web_postgres

# Xóa container (dữ liệu trong volume vẫn còn)
docker rm demo_web_postgres

# Xem logs realtime
docker logs -f demo_web_postgres

# Xem resource usage
docker stats demo_web_postgres

# Xem thông tin chi tiết container
docker inspect demo_web_postgres

# Xóa volume (XÓA DỮ LIỆU!)
docker volume rm demo_web_data
```

---

## Khởi Chạy Ứng Dụng

### Development Mode
```bash
# Khởi động PostgreSQL
docker-compose up -d postgres

# Chạy ứng dụng Spring Boot
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Mode
```bash
# Build ứng dụng
mvn clean package -DskipTests

# Chạy với production profile
java -jar target/demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Tài Liệu Tham Khảo

- [Docker Documentation](https://docs.docker.com/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Boot Data JPA](https://spring.io/guides/gs/accessing-data-jpa/)
- [Docker Compose](https://docs.docker.com/compose/)

---

## Liên Hệ & Hỗ Trợ

Nếu gặp vấn đề, vui lòng:
1. Kiểm tra phần [Xử Lý Sự Cố](#xử-lý-sự-cố)
2. Xem logs: `docker logs demo_web_postgres`
3. Kiểm tra connection string trong application-dev.properties

**Chúc bạn triển khai thành công! 🚀**

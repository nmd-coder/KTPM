# Giai đoạn 1: Build ứng dụng với Maven
# Sử dụng một image có sẵn Java 25 và Maven để build code
FROM maven:3.9.16-eclipse-temurin-25 AS build

# Đặt thư mục làm việc bên trong container
WORKDIR /app

# Copy file pom.xml trước để tận dụng cache của Docker
COPY pom.xml .

# Copy toàn bộ source code vào
COPY src ./src

# Chạy lệnh build của Maven để tạo ra file .jar
# -DskipTests để bỏ qua việc chạy test cho nhanh
RUN mvn clean package -DskipTests


# Giai đoạn 2: Chạy ứng dụng
# Sử dụng một image Java 25 nhỏ gọn hơn để chạy, giúp tiết kiệm dung lượng
FROM eclipse-temurin:25-jre

# Đặt thư mục làm việc
WORKDIR /app

# Copy file .jar đã được build từ giai đoạn 1 sang
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080 để bên ngoài có thể truy cập vào
EXPOSE 8080

# Câu lệnh để khởi động ứng dụng khi container chạy
ENTRYPOINT ["java","-jar","app.jar"]
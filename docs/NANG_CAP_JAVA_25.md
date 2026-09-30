# Nâng cấp Java lên phiên bản LTS 25

## Tổng quan

Dự án được chuyển từ Java 17 lên Java 25 LTS. Mục tiêu là dùng phiên bản Java LTS mới nhất tại thời điểm nâng cấp, đồng thời bảo đảm cấu hình build, Docker và các thư viện chính tương thích với runtime mới.

## Những thay đổi và lý do

| Thành phần | Thay đổi | Vì sao cần thay đổi | Tác động |
| --- | --- | --- | --- |
| `pom.xml` | `java.version` từ `17` lên `25` | Đặt mức Java mục tiêu cho biên dịch theo yêu cầu nâng cấp | Mã được build thành bytecode Java 25; môi trường chạy cần Java 25 trở lên |
| Spring Boot | Từ `3.5.5` lên `3.5.14` | Bản `3.5.6` ghi nhận hỗ trợ Java 25; sau đó nâng tiếp lên `3.5.14` để nhận các bản vá bảo mật được CVE scan phát hiện | Có cập nhật các dependency do Spring Boot quản lý; API và luồng nghiệp vụ của StarShop không được chủ động thay đổi |
| Maven compiler | Thêm `maven.compiler.proc=full` | JDK 21 trở lên yêu cầu khai báo rõ việc chạy annotation processor; Lombok dùng cơ chế này để sinh getter, setter, constructor và logger | Chỉ ảnh hưởng quá trình biên dịch; mã sinh ra vẫn phục vụ các annotation Lombok hiện có |
| PostgreSQL JDBC | Ghim phiên bản `42.7.12` | Bản BOM quản lý cũ nằm trong dải bị cảnh báo; phiên bản mới là bản vá được chọn trong quá trình quét CVE | Cập nhật driver kết nối PostgreSQL; không đổi schema hay dữ liệu |
| Docker build | `maven:3.8.5-openjdk-17` thành `maven:3.9.16-eclipse-temurin-25` | Build bên trong container bằng Maven 3.9.16 và JDK 25 | Docker image build dùng cùng thế hệ Java với cấu hình Maven |
| Docker runtime | `openjdk:17-jdk-slim` thành `eclipse-temurin:25-jre` | Chạy ứng dụng trên Java 25 và dùng image JRE cho runtime | Container production cần runtime Java 25 |
| README và hướng dẫn database | Cập nhật nhãn/yêu cầu Java 17 thành Java 25 | Tránh hướng dẫn người dùng cài Java cũ hơn mức dự án yêu cầu | Người phát triển cần cài JDK 25 để build/run trực tiếp |
| `DemoApplication` | Bỏ `@ComponentScan("com.example.demo")` bị lặp | `@SpringBootApplication` đã tự quét cùng package; khai báo thêm làm MVC test slice nạp cả cấu hình JPA ngoài ý muốn | Không đổi phạm vi package được quét trong ứng dụng; giúp kiểm thử web slice tách biệt persistence đúng cách |
| Hai test staff orders | Cô lập cấu hình JPA, mock dependency cần thiết, cấp dữ liệu trang rỗng và CSRF token cho request PUT | Các test MVC cần đủ dependency của controller advice/security nhưng không cần database thật | Chỉ thay đổi fixture kiểm thử; security filter và CSRF vẫn được bật |

## Ảnh hưởng tới sản phẩm

Không có thay đổi chủ động nào đối với nghiệp vụ, giao diện, API, luồng đơn hàng, schema cơ sở dữ liệu hoặc dữ liệu hiện có. Thay đổi ở `DemoApplication` chỉ bỏ cấu hình quét package trùng lặp; Spring Boot vẫn quét package `com.example.demo` như trước. Các thay đổi trong test chỉ giúp dựng đúng môi trường kiểm thử.

Ảnh hưởng vận hành cần lưu ý: máy chạy ứng dụng và container phải dùng Java 25 trở lên. Image runtime Java 17 cũ không còn phù hợp với artifact được build ở mức Java 25. Pipeline/deployment bên ngoài repository nếu có cũng cần được chuyển sang Java 25.

## Kiểm chứng

- Biên dịch sạch cả mã production và test trên JDK 25: thành công.
- Toàn bộ test suite trên JDK 25: thành công.
- Quét CVE cho 26 dependency trực tiếp đã xác định phiên bản: không còn cảnh báo cần xử lý.
- Build Docker image với image builder và runtime mới: thành công.

Không chạy được baseline trước nâng cấp vì máy không có JDK 17. Dự án hiện chưa cấu hình JaCoCo nên không có số liệu coverage trong lần nâng cấp này.

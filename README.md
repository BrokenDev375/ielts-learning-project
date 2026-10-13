# IELTS Study — hệ thống lớp học và ôn luyện IELTS

Dự án bài tập lớn **Lập trình mạng**, phát triển riêng trong thư mục này. Nhánh **feature/writing**, tạo trực tiếp từ main, có **module 1: tài khoản/lớp học** và **module 2: soạn, xuất bản, giao học liệu Writing**. Phần làm/nộp bài, AI, hồ sơ trình độ và gamification là bước tiếp theo.

Bản ORM mới khởi tạo ngày 13/10/2026: Spring Data JPA/Hibernate, Hibernate ddl-auto=validate, open-in-view=false. Docker project ielts-learning-orm và database/volume riêng. Module 1 ở main; Speaking và Writing phát triển riêng trên feature/speaking và feature/writing, chưa hợp nhất giao diện. Thay đổi stack tiếp theo phải thống nhất với người phụ trách trước khi triển khai.

## Chức năng hiện có

- Đăng ký tài khoản học viên, đăng nhập/đăng xuất và xem tài khoản.
- Tài khoản giảng viên được cấp bằng cấu hình demo, không chọn vai trò qua đăng ký công khai.
- Giảng viên tạo nhiều lớp, sửa tên/mô tả, xem thành viên, chia sẻ mã tham gia và xóa thành viên khỏi lớp mình.
- Học viên tham gia nhiều lớp bằng mã 8 ký tự, xem lớp mình và rời lớp.
- Server kiểm tra quyền trên tài nguyên; không chỉ ẩn nút ở frontend.
- Mật khẩu băm BCrypt, cookie session HttpOnly, CSRF token cho các yêu cầu thay đổi dữ liệu.
- Yêu cầu tạo lớp cùng requestId/nội dung được trả lại lớp cũ; tham gia/xóa/rời lớp không ghi trùng.
- MySQL lưu tài khoản/lớp/thành viên qua những lần chạy; Flyway quản lý schema.
- Giao diện responsive cho giảng viên/học viên, thông báo lỗi và giữ nội dung biểu mẫu khi yêu cầu chưa thành công.

## Writing hiện có

- Soạn Task 1 Academic/Task 2, tải hình PNG/JPEG có phân quyền, lưu nháp/xem trước/xuất bản và lịch sử phiên bản.
- Năm nhãn bổ trợ: chọn overview, ngôn ngữ mô tả, từ vựng chủ đề, liên kết ý, sửa ngữ pháp; có bước hướng dẫn và vận dụng.
- Giao cho cả lớp tại thời điểm giao hoặc từng học viên; người học nhận đúng phiên bản, không nhận bài mẫu/ghi chú riêng.
- Bảy bản nháp demo tự biên soạn; giảng viên kiểm tra trước khi phát, chưa có band đã được kiểm chứng.
- [Payload, seed, nguồn và cách demo](docs/WRITING_MODULE_2.md).

## Chạy nhanh bằng Docker trên Windows

Cần cài **Docker Desktop với Linux containers** và mở Docker Desktop, đợi engine sẵn sàng. Không cần cài Java, Node, Maven hoặc MySQL riêng để chạy bản Docker.

Trong PowerShell:

    cd "D:\Study\ky 1 nam 4\LTM\ielts-learning-orm"
    .\start.cmd

Lệnh tạo .env lần đầu, tạo mật khẩu database local, build và khởi động cả ba service, chờ healthcheck. Sau đó mở **http://localhost:5173**.

| Vai trò demo | Email | Mật khẩu mặc định trong .env.example |
|---|---|---|
| Giảng viên | teacher@ielts.local | Teacher@12345 |
| Học viên | student@ielts.local | Student@12345 |

Đây là tài khoản mẫu cho demo local. Có thể thay cấu hình DEMO_* trước lần chạy đầu tiên. Khi tài khoản đã tồn tại, khởi động lại không đặt lại mật khẩu của nó. DEMO_ENABLED=false tắt việc tạo tài khoản mẫu; không xóa những tài khoản đã lưu. Có thể tạo học viên mới bằng màn hình Đăng ký.

Nếu đã có .env, có thể chạy trực tiếp:

    docker compose up -d --build --wait

Linux/macOS: sh scripts/start.sh. Script này dùng .env.example nếu .env chưa tồn tại; chỉnh mật khẩu trong .env theo môi trường trước khi sử dụng ngoài demo local.

### Dừng, mở lại và xem log

    docker compose down
    docker compose up -d --wait
    docker compose ps
    docker compose logs --tail=100 server
    docker compose logs --tail=100 mysql

MySQL dùng named volume mysql_data; hình dùng asset_data. Dừng/recreate container bằng các lệnh trên giữ dữ liệu. Đăng nhập lại nếu session đã hết hạn hoặc backend được khởi động lại.

### Cổng và kết nối

- 5173: giao diện Nginx, proxy /api sang server.
- 8080: API backend, mặc định chỉ publish trên 127.0.0.1.
- MySQL không publish cổng trong compose.yml. compose.dev.yml chỉ publish 127.0.0.1:3306 để phát triển ở IDE.
- Đổi APP_PORT/API_PORT/MYSQL_PORT trong .env nếu cổng bị chiếm.
- Máy khác trong LAN mở http://IP_MAY_SERVER:5173; firewall của máy server cần cho phép cổng giao diện. Người dùng dùng cùng một địa chỉ giao diện để cookie session nhất quán.
- DB không được trình duyệt truy cập trực tiếp. Java truy cập dữ liệu qua Spring Data JPA/Hibernate bằng tài khoản ứng dụng; không dùng root.

## Chạy trực tiếp để phát triển

Yêu cầu Java 17+, Node 22.12+ (đã kiểm tra build trên Node 24), Docker/MySQL 8.4. Server có Maven Wrapper; frontend có package-lock.json.

Nếu đang chạy trọn bộ bằng Docker, dừng server/client trước khi chạy bằng IDE để giải phóng cổng: docker compose stop server client. Database và volume được giữ lại.

1. Khởi tạo cấu hình và chỉ chạy database:

       powershell -NoProfile -ExecutionPolicy Bypass -File scripts/init-env.ps1
       docker compose -f compose.yml -f compose.dev.yml up -d mysql

2. Ở terminal server, nhập thông tin database từ .env bằng biến môi trường, không đưa mật khẩu thật vào source:

       cd source/server
       $env:DB_USERNAME = "ielts_app"
       $env:DB_PASSWORD = "MAT_KHAU_MYSQL_PASSWORD_TRONG_ENV"
       $env:DEMO_ENABLED = "true"
       $env:DEMO_TEACHER_PASSWORD = "Teacher@12345"
       $env:DEMO_STUDENT_PASSWORD = "Student@12345"
       .\mvnw.cmd spring-boot:run

   DB_URL mặc định dùng 127.0.0.1:3306/ielts_learning_orm. Nếu đổi tên database/cổng, đặt DB_URL tương ứng. Email demo cũng cấu hình được qua DEMO_TEACHER_EMAIL/DEMO_STUDENT_EMAIL.

3. Terminal frontend:

       cd source/client
       npm ci
       npm run dev

Frontend Vite proxy /api sang 127.0.0.1:8080. Có thể đổi API_PROXY_TARGET nếu cần. Dùng http://localhost:5173 để truy cập giao diện.

## Database

Schema tại source/server/src/main/resources/db/migration: V1 tài khoản/lớp và V2 tài nguyên/giao bài (13 bảng mới). V2 giống nhánh Speaking, giữ nguyên khi hợp nhất; V3–V6 chưa triển khai. Flyway chạy khi server khởi động, không cần tự nhập SQL.

| Bảng | Vai trò |
|---|---|
| users | Email duy nhất, mật khẩu đã băm, tên, vai trò |
| classrooms | Chủ sở hữu, tên/mô tả, mã tham gia duy nhất, requestId chống tạo trùng |
| class_members | Quan hệ nhiều lớp–nhiều học viên; khóa chính kép chống tham gia trùng |
| flyway_schema_history | Lịch sử migration do Flyway quản lý |

V2 có assets, labels, resources/resource_versions, liên kết hình/nhãn, assignments/assignment_recipients và các bảng chuẩn bị trắc nghiệm/nghe. Writing dùng content_json/teaching_json; hình liên kết qua resource_assets.role=VISUAL. Chi tiết tại DATABASE_DESIGN.md.

Session hiện lưu trong bộ nhớ backend. Dữ liệu nghiệp vụ nằm trong MySQL, session hết hiệu lực khi backend restart.

## Kiến trúc và giao tiếp mạng

    Client giảng viên / học viên
                |
          HTTP + JSON
                |
         Nginx / Vite proxy
                |
       Spring Boot server
                |
       JPA / Hibernate
                |
              MySQL

Sơ đồ ở statics/architecture.mmd. Luồng giao tiếp ở statics/protocol.mmd và source/common/API.md.

- Client quản lý biểu mẫu, tương tác, lọc/hiển thị lớp và thống kê từ dữ liệu được phép nhận.
- Server xác thực, quản lý session, phân quyền lớp, xử lý request, lưu/cấp dữ liệu và kiểm tra trạng thái/khóa chống trùng.
- HTTP/TCP được trình duyệt, proxy và Tomcat quản lý; session đăng nhập độc lập với một connection TCP.
- Backend xử lý request trên các thread Tomcat; Hibernate dùng Hikari connection pool tối đa 10 kết nối. Transaction và unique constraints bảo vệ dữ liệu dùng chung.
- Timeout, dữ liệu sai, mất session, sai quyền và lỗi database trả lỗi có cấu trúc. Request tạo lớp được thử lại với cùng requestId khi chưa có xác nhận.
- Không có WebSocket/heartbeat/đồng bộ offline ở module này.

## Build và kiểm thử

Backend, yêu cầu Docker engine để Testcontainers tạo MySQL **riêng cho test**:

    cd source/server
    .\mvnw.cmd test
    .\mvnw.cmd package

Frontend:

    cd source/client
    npm ci
    npm run build
    npx playwright install chromium
    npm run test:e2e

E2E cần bản ứng dụng đang chạy tại http://127.0.0.1:5173, có tài khoản mẫu. Có thể cấu hình E2E_BASE_URL/E2E_TEACHER_EMAIL/E2E_TEACHER_PASSWORD/E2E_STUDENT_EMAIL/E2E_STUDENT_PASSWORD. PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH cho phép dùng Chrome đã có sẵn.

Bộ test API kiểm tra đăng nhập/đăng ký, BCrypt, CSRF, giả vai trò, truy cập lớp của người khác, nhiều lớp, chỉnh sửa/xóa/rời lớp, request trùng và 4 request đồng thời. Browser test dùng hai browser context độc lập cho giảng viên/học viên.

Nhánh Writing có 23 API test (12 module 1 + 11 Writing) và 7 browser test (3 module 1 + 4 Writing). [Kết quả Writing](statics/results/module-2-writing-validation.md); kết quả module 1 giữ riêng. Test chức năng chưa thay cho benchmark hiệu năng hoặc đánh giá hiệu quả học tập.

## Cấu trúc source

Cấu trúc package và trách nhiệm từng tầng: [BACKEND_STRUCTURE.md](docs/BACKEND_STRUCTURE.md).

    ielts-learning-orm/
      README.md
      INSTRUCTION.md
      TOPICS.md
      compose.yml
      compose.dev.yml
      .env.example
      start.cmd
      scripts/
      report/
      statics/
        architecture.mmd
        protocol.mmd
        results/
        dataset/
      source/
        client/       React, TypeScript, Vite, browser tests
        server/       Spring Boot, JPA/Hibernate, Flyway, API tests
        common/       Đặc tả HTTP/message và quyền

## Giới hạn và phần phát triển tiếp

- Hiện có tài khoản/lớp, học liệu/giao bài Writing và các bước bổ trợ được soạn sẵn; chưa có nhập/lưu nháp/nộp bài của học viên, AI, profile IELTS, đề xuất bổ trợ tự động hoặc XP.
- Session ở RAM của một backend; chưa có nhiều backend, quên mật khẩu, xác minh email hoặc hệ thống cấp tài khoản giảng viên thực tế.
- Danh sách hiện phù hợp prototype nhỏ, chưa có phân trang/rate limiting hoặc benchmark tải.
- Các cơ chế quyền, idempotency và Docker là phần nền tảng kỹ thuật. Không tự gọi chúng là novelty đã được chứng minh.
- Contribution dự kiến theo plan: AI hỗ trợ chấm có bằng chứng/giảng viên hiệu chỉnh và phân phối bài bổ trợ theo nhu cầu. Cần triển khai, baseline và số liệu thật để đánh giá.

## Nguồn và framework

- Spring Boot 3.5.16, Spring Security, Spring Data JPA/Hibernate, Hikari, Flyway; Java target 17.
- MySQL 8.4 và MySQL Connector/J.
- React 19.3.0, Vite 8.3.4, TypeScript 5.9.3; npm lockfile.
- Docker Compose, Maven/Temurin, Node và Nginx images.
- JUnit/AssertJ, Spring Boot Test, Testcontainers và Playwright.

Thư viện/version được khai báo trong pom.xml và package-lock.json. Không tự huấn luyện model hoặc dùng API AI trong module này.

Tài liệu:
- [Hướng dẫn môn học](INSTRUCTION.md), [topics](TOPICS.md), [API](source/common/API.md).
- [Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
- [Spring Security session authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/en/).
- [Docker Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/).
- [Vite](https://vite.dev/guide/).

## Kế hoạch và tài liệu môn học

- [Kế hoạch kiến trúc và các module](PLAN_IELTS_LEARNING_SYSTEM.md).
- [Thiết kế database, ERD và cấu trúc cột/API](DATABASE_DESIGN.md).
- [Instruction.md — hướng dẫn bài tập lớn](docs/course/Instruction.md).
- [Topics.md — danh sách chủ đề](docs/course/Topics.md).
- [Submission.md — yêu cầu nộp bài](docs/course/Submission.md).
- [README.md — bản gốc tại thư mục LTM](docs/course/README.md).

Bốn file trong docs/course được giữ nguyên nội dung từ thư mục LTM. README này là hướng dẫn chạy dự án IELTS.


Quy ước entity, repository, transaction và nguồn framework: [ORM_ARCHITECTURE.md](docs/ORM_ARCHITECTURE.md).

Backend đã tách interface/implementation cho service và repository tự viết trên cả ba nhánh; controller inject interface, lớp thực thi ở các package impl. Các *JpaRepository do Spring Data sinh implementation. [Cấu trúc backend](docs/BACKEND_STRUCTURE.md), [kiểm tra lần tách tầng](statics/results/backend-interfaces-validation.md).

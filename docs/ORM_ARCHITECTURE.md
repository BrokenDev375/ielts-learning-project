# ORM và quy ước triển khai

Stack đã chốt: React/TypeScript/Vite, Java 17/Spring Boot 3.5.16, Spring Data JPA/Hibernate, MySQL 8.4, Flyway, Docker Compose. Hỏi người phụ trách trước khi thay stack.

Các tầng nằm trong controller/service/repository/entity/dto; service và repository tự viết có interface, implementation ở các package impl; xem [BACKEND_STRUCTURE.md](BACKEND_STRUCTURE.md). Entity scan và repository scan nằm dưới package gốc vn.edu.ptit.ielts.

Code nghiệp vụ truy cập dữ liệu qua entity, JpaRepository và JPQL. MySQL Connector/J là driver bên dưới Hibernate; jdbc:mysql là URL kết nối, không có nghĩa ứng dụng dùng JdbcTemplate. Flyway vẫn dùng SQL migration để quản lí schema. Hibernate ddl-auto=validate kiểm tra mapping, open-in-view=false để việc đọc quan hệ nằm trong transaction.

Module 1 có UserEntity, ClassroomEntity và ClassMemberEntity; membership dùng EmbeddedId và MapsId. JpaRepository quản lí lưu/xóa, UserRepositoryImpl/ClassroomRepositoryImpl chuyển entity/query projection sang các record DTO đang có. API chỉ trả DTO, không trả entity chứa password hoặc quan hệ lazy.

Service tạo lớp khóa UserEntity của giảng viên trước khi kiểm tra UUID; service join/remove/leave khóa ClassroomEntity trước khi đổi membership. JPQL projection tải tên giảng viên/số thành viên/ngày tham gia trong query thay vì loop tải từng quan hệ. Constraint tại MySQL giữ unique email/code/request và PK membership.

Một transaction Hibernate đã lỗi constraint cần rollback; không bắt lỗi rồi tiếp tục query trong cùng transaction. Các lỗi trùng đăng ký được chuyển thành 409 sau khi transaction repository rollback. Race mã lớp giữa hai giảng viên hiếm có vẫn bị unique constraint chặn và trả conflict để gửi lại.

Docker project: ielts-learning-orm, database ielts_learning_orm, volume riêng. Frontend 5173, API host 8080, cổng MySQL IDE 3306; bên trong Docker server dùng 8080. Bản cũ ở ielts-learning-project là nguồn tham khảo, không nằm trong lịch sử Git mới.

Nguồn kỹ thuật:

- [Spring Boot 3.5 — SQL/JPA](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html).
- [Spring Data JPA 3.5 — Transactions](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/transactions.html).
- [Spring Data JPA 3.5 — Locking](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/locking.html).

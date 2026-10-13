# ORM và quy ước triển khai

Stack đã chốt: React/TypeScript/Vite, Java 17/Spring Boot 3.5.16, Spring Data JPA/Hibernate, MySQL 8.4, Flyway, Docker Compose. Hỏi người phụ trách trước khi thay stack.

Các tầng nằm trong controller/service/repository/entity/dto; service và repository tự viết có interface, implementation ở các package impl; xem [BACKEND_STRUCTURE.md](BACKEND_STRUCTURE.md). Entity scan và repository scan nằm dưới package gốc vn.edu.ptit.ielts.

Code nghiệp vụ truy cập dữ liệu qua entity, JpaRepository và JPQL. MySQL Connector/J là driver bên dưới Hibernate; jdbc:mysql là URL kết nối, không có nghĩa ứng dụng dùng JdbcTemplate. Flyway vẫn dùng SQL migration để quản lí schema. Hibernate ddl-auto=validate kiểm tra mapping, open-in-view=false để việc đọc quan hệ nằm trong transaction.

Module 1 có UserEntity, ClassroomEntity và ClassMemberEntity; membership dùng EmbeddedId và MapsId. JpaRepository quản lí lưu/xóa, UserRepositoryImpl/ClassroomRepositoryImpl chuyển entity/query projection sang các record DTO đang có. API chỉ trả DTO, không trả entity chứa password hoặc quan hệ lazy.

Service tạo lớp khóa UserEntity của giảng viên trước khi kiểm tra UUID; service join/remove/leave khóa ClassroomEntity trước khi đổi membership. JPQL projection tải tên giảng viên/số thành viên/ngày tham gia trong query thay vì loop tải từng quan hệ. Constraint tại MySQL giữ unique email/code/request và PK membership.

Một transaction Hibernate đã lỗi constraint cần rollback; không bắt lỗi rồi tiếp tục query trong cùng transaction. Các lỗi trùng đăng ký được chuyển thành 409 sau khi transaction repository rollback. Race mã lớp giữa hai giảng viên hiếm có vẫn bị unique constraint chặn và trả conflict để gửi lại.

Docker project: ielts-learning-orm, database ielts_learning_orm, volume riêng. Frontend 5173, API host 8080, cổng MySQL IDE 3306; bên trong Docker server dùng 8080. Bản cũ ở ielts-learning-project là nguồn tham khảo, không nằm trong lịch sử Git mới.

Module 2 có ResourceEntity, ResourceVersionEntity, AssetEntity, LearningLabelEntity, AssignmentEntity và ba entity liên kết: ResourceAssetEntity, ResourceLabelEntity, AssignmentRecipientEntity. Nội dung/teaching/source ánh xạ JsonNode vào cột MySQL JSON bằng Hibernate; teaching chỉ đưa vào DTO giảng viên. Các entity liên kết dùng EmbeddedId/MapsId.

LearningRepository là facade trên 8 JpaRepository mới, trả DTO cho service/API. LearningService khóa ResourceEntity khi sửa/xuất bản; Assignment khóa ClassroomEntity để chụp thành viên và kiểm tra UUID/fingerprint. Giao bài dùng READ_COMMITTED để yêu cầu đồng thời đọc được kết quả sau khi nhận khóa. Khóa lớp cũng được dùng trong thay đổi membership của module 1.

Phiên bản PUBLISHED không sửa trực tiếp; lần sửa tạo phiên bản DRAFT kế tiếp. Assignment giữ khóa phiên bản nên đề đã giao không bị đổi. File audio ở volume riêng, metadata/quyền qua JPA; rollback giao dịch xóa file vừa tạo. Seed khóa UserEntity, kiểm tra marker của từng loại và không tạo lại bài/audio qua restart.

Migration V2 tạo 13 bảng, trong đó 5 bảng câu hỏi/lựa chọn/nguồn nghe/đoạn/transcript dành cho các kỹ năng tiếp theo chưa có entity hoặc endpoint nội dung. Flyway là nơi định nghĩa schema; không dùng Hibernate create/update hoặc nhập schema tham chiếu vào database ứng dụng.

Nguồn kỹ thuật:

- [Spring Boot 3.5 — SQL/JPA](https://docs.spring.io/spring-boot/3.5/reference/data/sql.html).
- [Spring Data JPA 3.5 — Transactions](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/transactions.html).
- [Spring Data JPA 3.5 — Locking](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/locking.html).

## Module 2 Writing

Entity chung V2: Resource, ResourceVersion, ResourceAsset, ResourceLabel, Assignment, AssignmentRecipient, Asset và Label. Service khóa version khi lưu/xuất bản và khóa lớp khi giao để chốt người nhận; unique/requestId chống giao trùng. Metadata hình ở MySQL, byte ở asset_data; upload rollback dọn tệp vừa ghi. WritingPayloads/WritingContentHandler/DemoWritingInitializer nằm ở các tầng tương ứng, không tạo schema riêng cho Writing.

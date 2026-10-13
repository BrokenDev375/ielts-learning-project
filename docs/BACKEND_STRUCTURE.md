# Cấu trúc backend: interface và implementation

Controller → service interface → service.impl → repository interface → repository.impl → Spring Data JPA → MySQL.

    vn/edu/ptit/ielts/
      controller/
      service/
        AuthService.java
        ClassroomService.java
        impl/
          AuthServiceImpl.java
          ClassroomServiceImpl.java
      repository/
        UserRepository.java
        ClassroomRepository.java
        UserJpaRepository.java
        ClassroomJpaRepository.java
        ClassMemberJpaRepository.java
        impl/
          UserRepositoryImpl.java
          ClassroomRepositoryImpl.java
      entity/
      dto/
      enums/
      config/
      security/
      exception/
      bootstrap/

- service chứa hợp đồng các phương thức nghiệp vụ; controller và bootstrap nhận interface qua constructor.
- service.impl chứa @Service và logic nghiệp vụ. @Transactional giữ trên implementation và từng phương thức cần ghi/khóa.
- repository chứa hợp đồng truy cập dữ liệu tự viết và các interface *JpaRepository.
- repository.impl chứa @Repository, transaction và phần ánh xạ DTO/phối hợp truy vấn JPA.
- Các *JpaRepository extends JpaRepository; Spring Data tạo implementation lúc khởi động.
- entity ánh xạ bảng/quan hệ; API trả DTO, không serialize entity chứa password hoặc lazy relations.
- dto chứa request/view/record; UserRecord có passwordHash chỉ dùng nội bộ, HTTP trả UserView.
- security/AuthValidation chứa chuẩn hóa email/giới hạn mật khẩu UTF-8 dùng chung trong login, đăng ký và seed.

Module 1: AuthController/ClassroomController/HealthController; hai service interface và hai repository tự viết có implementation riêng.

Trên hai nhánh kỹ năng có thêm LearningService, AssetService, LearningRepository và các implementation tương ứng. ResourceContentHandler là interface kiểm tra nội dung; SpeakingContentHandler/WritingContentHandler nằm trong service.impl. ResourceAccess chứa kiểm tra vai trò/lỗi dùng chung cho tài nguyên.

Entity, schema/migration, JPQL projection, JSON và endpoint giữ nguyên sau lần tách tầng. Các record Created/Joined nằm trong ClassroomService để giữ hợp đồng trả về. Có một implementation cho mỗi interface được inject.

Lịch sử có main chứa một commit module 1; feature/speaking và feature/writing mỗi nhánh thêm một commit từ main. Toàn repository có ba commit, mỗi nhánh kỹ năng có hai commit từ gốc. Frontend 5173, backend 8080, MySQL IDE 3306.

Kết quả kiểm tra lần tách tầng: [backend interfaces validation](../statics/results/backend-interfaces-validation.md).

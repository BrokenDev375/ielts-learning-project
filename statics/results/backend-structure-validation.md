# Kiểm tra backend sau khi đổi cấu trúc package

Ngày 13/10/2026. Phạm vi: chuyển 59 class Java sang các tầng và cập nhật import/JPQL, dựng hai snapshot module 1 và Speaking cho lịch sử Git mới.

| Kiểm tra | Kết quả |
|---|---|
| Module 1 chạy độc lập, schema V1, 29 class/3 JpaRepository | 12/12 API test PASS |
| Module 1 + Speaking, schema V1/V2, 59 class/11 JpaRepository | 20/20 API test PASS: 12 module 1 + 8 Speaking |
| JPA mapping/JPQL constructor DTO sau khi đổi package | Hibernate validate và Spring context khởi động thành công |
| Scan package cũ trong Java main/test | Không còn auth/classroom/learning/speaking/common |
| Migration và JSON API | Giữ nguyên V1/V2 và hợp đồng HTTP/JSON |

Kiểm thử Speaking bao gồm quyền tài liệu/audio, DTO học viên không chứa teaching, phiên bản đã giao bất biến, snapshot/chọn người nhận, mất membership mất quyền truy cập, idempotency và yêu cầu đồng thời, thời gian mở bài, upload và Range audio. Fixture dùng MySQL Testcontainers riêng.

Các kiểm tra giao diện/persistence ở speaking-module-2-validation.md được thực hiện trước lần đổi package. Lần này tập trung kiểm tra backend và cấu trúc Git; không thay đổi frontend.

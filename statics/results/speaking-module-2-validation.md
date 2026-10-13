# Kiểm tra Speaking module 2 — bản ORM

Ngày: 13/10/2026. Phạm vi: tạo/xuất bản/giao bài Speaking, dữ liệu/API dùng chung, audio và quyền truy cập. Đây là kiểm tra chức năng, không phải đánh giá chất lượng AI hoặc hiệu quả học tập.

| Kiểm tra | Kết quả |
|---|---|
| Backend Maven, Java target 17 | Build thành công; Docker build bằng JDK 17 |
| ORM và mapping | Hibernate 6.6.53.Final, ddl-auto=validate, 11 JpaRepository; không có JdbcTemplate/SQL trực tiếp trong Java nghiệp vụ |
| API MySQL 8.4.9/Testcontainers | 20/20 qua: 12 module 1, 8 Speaking/module 2 |
| Frontend TypeScript/Vite | Build thành công trong Docker |
| Browser Playwright/Chrome | 6/6 qua: 3 module 1, 3 Speaking |
| Docker Compose riêng tại cổng 5186 | MySQL/server/client đều healthy |
| Migration database đang có V1 sang V2 | PASS; tài khoản/lớp/thành viên đã lưu từ module 1 còn nguyên |
| Giữ dữ liệu khi recreate server/client | PASS: 5 tài liệu, 2 assignment học viên, 5 audio có checksum giữ nguyên, 3 seed không sinh thêm |
| Dữ liệu tài khoản/lớp sau lần recreate có Speaking | PASS qua scripts/check-persistence.ps1 |

Kiểm tra API bao gồm vai trò/chủ bài/chủ lớp, chống đọc/sửa tài liệu người khác, nội dung xuất bản cần đủ dữ liệu, không nhận audio của giảng viên khác, bài được giao giữ phiên bản cũ, student DTO không chứa teaching, snapshot người nhận/chọn riêng, người vào lớp sau không tự nhận bài, mất membership mất quyền tải, yêu cầu giao trùng và hai yêu cầu đồng thời, thời gian mở bài và Range audio.

Browser test thực hiện từ giao diện: tạo nháp → xem trước → xuất bản → giao cả lớp; học viên đọc đúng đề; sửa/xuất bản phiên bản 2 mà assignment cũ không đổi. Luồng phát âm tải WAV thật, giao một học viên được chọn và xác nhận trình duyệt tải/giải mã audio (readyState >= 2). Mobile 390×844 không tràn ngang ở ngân hàng và form phát âm.

Các lần kiểm tra dùng database/container riêng, không dùng MySQL development để làm fixture. Sau kiểm tra dữ liệu, đăng nhập lại để xác nhận phiên session mới đọc được dữ liệu đã lưu. Fixture và kiểm tra số bản ghi API dùng JpaRepository.

Ảnh từ lượt browser test bản ORM đã qua:

- [Giảng viên xem phiên bản](../screenshots/speaking-teacher-desktop.png)
- [Học viên xem bài và audio](../screenshots/speaking-student-audio.png)
- [Ngân hàng trên điện thoại](../screenshots/speaking-mobile-bank.png)

API test: source/server/src/test/java/vn/edu/ptit/ielts/ModuleTwoSpeakingApiTest.java.
Browser test: source/client/e2e/speaking.spec.ts.
Kiểm tra persistence: source/client/scripts/verify-speaking-persistence.mjs capture/verify.

Giới hạn: audio seed là giọng tổng hợp và học liệu tự biên soạn cần giảng viên duyệt; chưa đánh giá phát âm/AI, chưa có viết outline/ghi âm/nộp bài hoặc chấm/profile. Hạn nộp/số lần làm/chính sách phản hồi mới được lưu cho module nộp bài tiếp theo. POST tạo tài liệu chưa có idempotency key, bản nháp dùng lần lưu cuối. Chỉ handler Speaking được triển khai; bảng chung Reading/Listening đã có nhưng chưa có authoring/API nội dung của hai kỹ năng này.

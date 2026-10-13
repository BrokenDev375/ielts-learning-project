# Kiểm tra module 2 Writing

Ngày chạy: **13/10/2026**. Nhánh feature/writing từ main; JPA/Hibernate, MySQL 8.4, Flyway V1/V2.

| Kiểm tra | Kết quả thực tế |
|---|---|
| Backend Maven test, MySQL Testcontainers riêng | 23 qua, 0 lỗi/skip: 12 module 1 + 11 Writing |
| Frontend npm ci / npm run build | Qua, TypeScript và Vite production build |
| Docker build Java target 17, Node 24, Nginx | Qua; ba service healthy |
| Chrome Playwright | 7 qua: 3 module 1 + 4 Writing |
| Persistence sau force-recreate server/client | PASS: 12 tài nguyên Writing gồm 7 seed, 5 bài giao/5 bài nhận, 3 checksum hình giữ nguyên |
| Seed sau restart | Đúng 7 bài mẫu, không tạo trùng, vẫn DRAFT |

Bộ browser chạy trên Docker project **ielts-writing-check** với database và asset volume riêng, dùng đúng cổng 5173/8080. Số 12 tài nguyên/5 bài giao/3 hình bao gồm fixture của các lượt test, không phải quy mô dữ liệu seed. Session mới sau restart, không lưu lại mật khẩu/cookie trong snapshot.

API kiểm tra quyền/CSRF/cross-teacher, Task 1/Task 2, năm nhãn và task phù hợp, điều kiện xuất bản, câu ngữ pháp cần xác nhận, private teaching không trả cho học viên, hình thực/giới hạn dung lượng/Range, lịch mở bài, phiên bản bất biến, lớp/nhóm nhận, thu hồi quyền khi rời lớp, request trùng/đồng thời và dữ liệu seed.

Browser kiểm tra tạo–preview–publish–assign–student view Task 2, giữ bản cũ sau sửa/xuất bản; tải PNG Task 1 thật và học viên giải mã hình 900px; liên kết ý có bước hướng dẫn/vận dụng và không lộ gợi ý riêng; bank/editor ở viewport 390px không tràn ngang. Một lượt đầu bộ test chọn sai ô lặp lại, đã sửa locator theo hoạt động; lượt cuối cả 7 test qua.

Ảnh chụp:

- [Giảng viên](writing-teacher-desktop.png).
- [Học viên xem hình Task 1](writing-student-chart.png).
- [Bài bổ trợ liên kết ý](writing-student-remedial.png).
- [Giao diện mobile](writing-mobile-bank.png).

Tái lập: Maven test, npm run build, npm run test:e2e theo README; script source/client/scripts/verify-writing-persistence.mjs capture/verify quanh lần recreate container. Chạy browser/persistence trên database demo riêng vì test tạo dữ liệu.

Giới hạn: đây là kiểm thử chức năng/quyền/persistence, chưa phải benchmark, band IELTS đã được xác nhận hoặc đo hiệu quả bài bổ trợ. Chưa có bài làm/nộp, AI chấm, profile và gợi ý bổ trợ tự động.

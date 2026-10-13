# Kiểm tra merge main vào Speaking

Ngày chạy: **13/10/2026**. Base main: 90c41b3 (PR #1 Writing); Speaking: b9396b5.

Đã giải quyết 17 file conflict. Nền shared giữ hỗ trợ audio và hình của main; thêm handler/seed/editor Speaking, tổng quát hóa kiểu chung, tách DTO Speaking và gom danh sách bài học viên. Hai ngân hàng giảng viên độc lập; preview học viên chọn theo skill, không trả teaching.

| Kiểm tra | Kết quả |
|---|---|
| Maven test / MySQL Testcontainers | 31 qua: 12 module 1 + 8 Speaking + 11 Writing; 0 lỗi/skip |
| npm ci / TypeScript / Vite build | PASS |
| Docker Java 17 build, schema validate, seed hai kỹ năng | PASS, 3 service healthy |
| Chrome Playwright | 11 qua: 3 module 1 + 3 Speaking + 4 Writing + 1 tích hợp |
| Speaking persistence sau force-recreate server/client | 5 tài nguyên gồm 3 seed, 7 bài nhận hai kỹ năng, 5 checksum audio giữ nguyên |
| Writing persistence sau force-recreate server/client | 10 tài nguyên gồm 7 seed, 4 bài giao/nhận Writing, 2 checksum hình giữ nguyên |
| Migration V1/V2 | Giữ nguyên nội dung; không thêm migration mới |

Browser test mới xác nhận cùng một lớp/học viên nhận cả hai kỹ năng, ngân hàng giảng viên lọc đúng, học viên lọc kỹ năng và xem đúng schema, không có mục tham khảo riêng, mobile không tràn. Các test cũ vẫn kiểm tra phiên bản bất biến, quyền, upload/phát audio, hình Task 1 và bài bổ trợ Writing.

Bộ Docker kiểm thử tên ielts-merge-check, dùng đúng cổng 5173/8080 và database/volume riêng. Số tài nguyên/bài giao bao gồm fixture kiểm thử, không phải số seed. Seed thực tế: 3 Speaking + 7 Writing, không tạo trùng sau restart.

[Ảnh danh sách bài của học viên](combined-student-assigned.png).

Tái lập: Maven test, npm run build, npm run test:e2e theo README. Hai script source/client/scripts/verify-speaking-persistence.mjs và verify-writing-persistence.mjs capture/verify quanh lần recreate container. Browser test tạo dữ liệu, cần dùng database demo kiểm thử riêng.

Phạm vi vẫn là module 2 authoring/assignment; chưa có nhập/nộp bài học viên, ghi âm bài nói hoặc AI chấm. Đây là kiểm thử chức năng/quyền/persistence, chưa phải benchmark hoặc hiệu quả học tập.

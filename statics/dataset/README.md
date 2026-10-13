# Dữ liệu demo

Hai tài khoản mẫu được DemoInitializer tạo khi DEMO_ENABLED=true, theo cấu hình .env. Mật khẩu băm trước khi lưu; không có file SQL chứa mật khẩu người dùng thật.

Trên feature/writing, DemoWritingInitializer tạo bảy học liệu DRAFT: Task 1, Task 2 và năm bài bổ trợ. Chart/CSV Riverton, đề, từ/câu, hoạt động và bài tham khảo được tự biên soạn cho demo, chưa có band đã được kiểm chứng. [Chi tiết seed/nguồn](../../docs/WRITING_MODULE_2.md).

Ban đầu chưa có lớp/bài giao. Giảng viên tạo lớp, duyệt học liệu rồi giao; học viên tham gia qua giao diện. Restart không tạo trùng seed còn marker DEMO_WRITING_V1.

Testcontainers dùng database/container riêng và fixture riêng. Browser tests tạo lớp/học viên thử trong database demo mà chúng đang chạy; không dùng E2E trên dữ liệu sử dụng thực tế.

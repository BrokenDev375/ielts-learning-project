# Speaking — phạm vi module 2

Nhánh feature/speaking triển khai tạo, xuất bản và giao bài; học viên nhận đúng nội dung/audio. Làm outline, ghi âm/nộp bài, AI, giảng viên chấm và profile thuộc các module tiếp theo.

## Phần dùng chung cho nhóm

- Migration V2__resources_and_assignments.sql tạo toàn bộ 13 bảng V2 và danh mục 16 nhãn đúng thiết kế. Giữ nguyên V1 đã áp dụng. Không tạo lại các bảng resources/assignments hoặc nhập schema.mysql.sql vào database ứng dụng.
- Dữ liệu qua entity/JpaRepository/JPQL; xem [quy ước ORM](ORM_ARCHITECTURE.md). Service giữ transaction và khóa hàng bằng JPA; không truy vấn SQL trực tiếp trong code nghiệp vụ.
- Nội dung dùng chung theo tầng: dto/ResourceRequests và LearningViews; repository/LearningRepository; service/LearningService; controller/LearningController. AssetService/AssetController nằm cùng tầng tương ứng. Xem [cấu trúc backend](BACKEND_STRUCTURE.md).
- ResourceContentHandler là điểm mở rộng cho từng kỹ năng. Hiện có SpeakingContentHandler; Reading/Listening/Writing bổ sung handler, kiểm tra JSON và liên kết riêng. API trả SKILL_NOT_SUPPORTED cho kỹ năng chưa có handler.
- source/client/src/features/learning giữ hợp đồng hiện tại và form giao bài; features/speaking giữ editor/preview/ngân hàng. Khi thêm kỹ năng, tổng quát hóa type content có phân biệt skill, không ép nội dung kỹ năng khác thành SpeakingContent.
- API đọc/lưu lựa chọn/Attempt/Submission ở plan 6.4 triển khai trong module 3; hiện không tạo giả endpoint làm/nộp bài.
- Mọi thành viên thống nhất số migration mới; không sửa V1/V2 sau khi Flyway đã áp dụng. Nên đưa nền chung vào PR sớm để các nhánh còn lại tích hợp.

## Nội dung Speaking schema v1

ResourceInput: title, skill=SPEAKING, resourceKind, activityType, topic, levelFramework, levelLabel, supportLevel, content, teaching, source.

| content.taskType | resourceKind | activityType | Nhãn tài liệu |
|---|---|---|---|
| MAIN | MAIN | SPEAKING_RECORDING | Không tự gán nhóm bổ trợ |
| DEVELOPMENT | REMEDIAL | OUTLINE | SPEAK_DEVELOPMENT |
| PRONUNCIATION | REMEDIAL | PRONUNCIATION | SPEAK_PRONUNCIATION |

content gồm part (0=tình huống riêng, 1–3=Part), prompt, instructions, preparationSeconds, speakingSeconds, checklist[], assessmentCriteria[], phonemes[], items[]. Mỗi item có text, ipa, audioAssetId (nullable khi nháp). Đây là tiêu chí/hướng dẫn soạn bài; rubric theo band và quy trình chấm hoàn chỉnh được tích hợp ở module chấm/AI.

teaching gồm outlineExamples[] và teacherNotes; chỉ giảng viên sở hữu được đọc. source gồm url và note; URL phải là HTTP/HTTPS hoặc trống. Source là thông tin công khai của bài, không đặt ghi chú riêng tại đây.

GET assignment detail theo format chung plan 6.4: assignmentId, resourceId, resourceVersionId, skill, title, dueAt, content, questions; Speaking trả questions rỗng, thêm metadata phiên bản và assignment. Phần student DTO giữ content và source, bỏ toàn bộ teaching. Mẫu outline không được dùng làm đáp án duy nhất. Hai nhãn bổ trợ tài liệu giúp tìm bài; không tự tạo nhãn điểm yếu cho học viên.

## Nháp, xuất bản, phiên bản

- POST resources tạo bài với phiên bản 1 DRAFT. PUT draft cập nhật bản nháp gần nhất.
- Khi gần nhất là PUBLISHED, PUT draft tạo phiên bản kế tiếp DRAFT. Phiên bản đã xuất bản giữ nguyên nội dung, labels và assets.
- Publish kiểm tra đề/tiêu chí; DEVELOPMENT cần checklist; PRONUNCIATION cần âm mục tiêu và ít nhất một từ/câu đủ text/ipa/audio.
- Audio phải thuộc giảng viên sở hữu và còn READY. Nội dung/schema không được phép thêm trường tùy ý.
- GET versions trả lịch sử; giảng viên có thể giao một phiên bản PUBLISHED cũ. Bài đã giao không bị đổi khi xuất bản phiên bản mới.
- Hai tab giảng viên cùng sửa một bản nháp hiện dùng cơ chế lần lưu cuối; chưa có thông báo xung đột giữa hai trình soạn.
- POST tạo tài liệu chưa có requestId chống tạo trùng; sau timeout cần tải lại ngân hàng kiểm tra. Form giao bài có requestId và kiểm tra fingerprint.

## Giao bài và quyền

Assignment cố định resourceVersionId, classroomId, purpose, recipientMode, studentIds, opensAt/dueAt (UTC), allowLate, maxAttempts và feedbackPolicy.

CLASS_SNAPSHOT chụp người học hiện tại; studentIds phải rỗng. SELECTED chọn ít nhất một thành viên đang thuộc lớp. Bài bổ trợ giao với purpose=REMEDIAL. Server kiểm tra chủ lớp/chủ bài và trạng thái xuất bản.

- Học viên tham gia lớp sau thời điểm giao không tự được thêm vào assignment.
- Học viên rời/bị xóa khỏi lớp mất quyền tải bài và audio; các bản ghi người nhận được giữ cho lịch sử sau này.
- Học viên chỉ đọc assignment mình nhận; nội dung/audio chưa đến opensAt bị chặn trên server.
- dueAt/maxAttempts/feedbackPolicy được lưu ở module 2; xử lí nộp và mở phản hồi dùng chúng ở module 3.
- Gửi lại cùng requestId/nội dung trả assignment cũ; đổi nội dung cùng ID trả 409. Khóa transaction và unique constraint bảo vệ yêu cầu đồng thời.
- Mở lại trang/Làm mới tải dữ liệu đã lưu qua HTTP; session RAM yêu cầu đăng nhập lại sau restart.

## Tệp và Docker

POST /api/assets dùng multipart/form-data trường file và CSRF token. Giảng viên tải WAV/MP3/OGG tối đa 5 MiB; server kiểm tra dấu hiệu định dạng đầu tệp và lưu checksum. GET /api/assets/{id} kiểm tra quyền và hỗ trợ Range cho audio. Không trả storage_key cho client.

Audio ở named volume asset_data, metadata ở MySQL; giữ cả hai khi backup/restore. Chạy ngoài Docker có thể đặt ASSET_DIRECTORY. Kho tệp không nằm trong static public. Upload chưa gắn vào bài không có tác vụ dọn định kỳ trong bản đầu.

## Seed và nguồn

DEMO_ENABLED=true thêm một bài nói chính, một bài phát triển câu trả lời và một bài phát âm /θ/ ở trạng thái DRAFT cho giảng viên demo. Nội dung/outline là mẫu tự biên soạn theo ví dụ trong plan, chưa là đề IELTS chuẩn hoặc học liệu chuyên môn đã duyệt. Giảng viên kiểm tra rồi xuất bản.

Bài phát âm có think, thin, three và một câu hoàn chỉnh. Audio WAV được tổng hợp trên Windows bằng Microsoft Zira Desktop, rate=-2, biến thể US; phiên âm cần đối chiếu khi duyệt. Không gọi API AI/ASR hoặc huấn luyện mô hình. Seed kiểm tra từng loại để bổ sung phần thiếu, giữ nội dung đã có qua restart.

Nguồn tham khảo chuyên môn giữ ở [plan mục 13.6](../PLAN_IELTS_LEARNING_SYSTEM.md): [Speaking criteria](https://ielts.org/take-a-test/test-types/ielts-academic-test/ielts-academic-format-speaking), [Speaking descriptors PDF](https://ielts.org/cdn/ielts-guides/ielts-speaking-band-descriptors.pdf), [British Council lesson plans](https://takeielts.britishcouncil.org/teach-ielts/teaching-resources/lesson-plans-speaking). Bản này không tải/copy audio từ các nguồn đó.

## Chạy và kiểm tra

Chạy start.cmd; giảng viên dùng Bài Speaking, học viên dùng Bài được giao. Bật demo/tạo lớp/thêm học viên theo README.

Backend: mvnw.cmd test. Frontend: npm run build; npm run test:e2e với ứng dụng có tài khoản demo và database thử.

scripts/verify-speaking-persistence.mjs (trong source/client) hỗ trợ capture/verify tài liệu, assignment và checksum audio qua API trước/sau recreate container. Dùng E2E_BASE_URL và SPEAKING_STATE_FILE nếu đổi địa chỉ/tệp kết quả.

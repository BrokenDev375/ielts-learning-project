# Writing — module 2: học liệu và giao bài

Writing được merge vào main qua PR #1. Nhánh feature/speaking hiện đã tích hợp main; hai kỹ năng dùng chung migration V2 và hợp đồng Resource/Assignment. Giảng viên có hai ngân hàng, học viên xem chung Bài được giao và lọc theo kỹ năng.

## Phạm vi hiện có

- Giảng viên tạo/sửa bản nháp, xem trước, xuất bản, xem lịch sử phiên bản.
- Task 1 Academic có hình PNG/JPEG, loại hình, đơn vị, nhóm dữ liệu và mốc thời gian. Task 2 có đề, hướng dẫn, checklist và bảng từ nếu được cấp.
- Bài chính dùng tối thiểu 150/250 từ và bốn tiêu chí đúng nhiệm vụ. Giới hạn thời gian là cấu hình luyện tập, chưa chạy đồng hồ thi.
- Giao bản đã xuất bản cho cả lớp tại thời điểm giao hoặc nhóm học viên được chọn; lưu thời gian mở/hạn nộp, số lượt, chính sách mở phản hồi.
- Học viên nhận đúng phiên bản đã giao. Xóa/rời lớp thu hồi quyền truy cập hiện tại, vẫn giữ hàng người nhận cho lịch sử.
- Hình chỉ được đọc bởi chủ sở hữu hoặc học viên đang thuộc lớp, có bài được giao liên kết hình và đã đến thời gian mở.
- teaching gồm bài mẫu, gợi ý đáp án và ghi chú chỉ dành cho giảng viên; response học viên loại bỏ toàn bộ trường này.

Phần nhập bài viết của học viên, đếm từ, lưu nháp/nộp bài thuộc module 3. Phân tích AI, rubric có phiên bản và chốt điểm thuộc module đánh giá. Chưa tích hợp API AI, chưa chấm band hoặc suy ra điểm yếu tự động.

## Năm nhãn bổ trợ

| Mã | Task | Nội dung thực hành |
|---|---|---|
| OVERVIEW_SELECTION | 1 | Chọn đặc điểm nổi bật, đối chiếu overview, viết overview cho ngữ cảnh mới |
| DESCRIPTIVE_LANGUAGE | 1 | Biến đổi câu mô tả xu hướng/tỷ lệ/so sánh, tránh lặp từ, vận dụng vào hình mới |
| TOPIC_VOCABULARY | 2 | Bảng từ có nghĩa/collocation/ví dụ; chọn và dùng từ vào câu theo đề |
| IDEA_LINKING | 1 và 2 | Xác định quan hệ ý, nối câu đúng quan hệ, vận dụng với hai ý mới |
| GRAMMAR_REPAIR | 1 và 2 | Sửa câu lỗi đã xác nhận, giải thích và giữ ý định gốc, sửa câu mới |

Bài bổ trợ cần checklist, ít nhất hai hoạt động, hoạt động đúng nhãn và bước vận dụng. teaching.sampleAnswers có ít nhất một phương án kèm giải thích. Phương án mở có thể có nhiều cách sửa hợp lệ; không dùng một câu mẫu làm đáp án duy nhất. TASK_RESPONSE và ARGUMENT_DEVELOPMENT còn là đề xuất trong plan, chưa thêm vào danh mục.

GRAMMAR_REPAIR cần originalSentence, originalMeaning, grammarSourceKind và teacherConfirmed=true mới xuất bản được. Ở module 2, câu do giảng viên cung cấp hoặc tự biên soạn cho demo; khi có bài nộp/nhận xét ở module sau sẽ bổ sung liên kết annotation nguồn. Câu seed để teacherConfirmed=false để giảng viên kiểm tra qua giao diện.

## Hợp đồng nội dung

ResourceInput chung: title, skill=WRITING, resourceKind, activityType, topic, levelFramework, levelLabel, supportLevel, content, teaching, source. Bài chính: MAIN/WRITING_ESSAY; bổ trợ: REMEDIAL/GUIDED. contentSchemaVersion=1 lưu phía server.

| Nhóm | Các trường |
|---|---|
| Nhiệm vụ | taskType (TASK_1/TASK_2), activity, prompt, instructions, minimumWords, writingSeconds |
| Hình | visualAssetId, visualType, hasTimeDimension, visualUnit, visualGroups[], visualPeriods[] |
| Hướng dẫn | checklist[], assessmentCriteria[], vocabulary[], activities[] |
| Ngữ pháp | originalSentence, originalMeaning, grammarSourceKind (DEMO_AUTHORING/TEACHER_SUPPLIED), teacherConfirmed |
| Riêng cho giảng viên | teaching.teacherNotes, modelResponse, sampleAnswers[], rubricSourceUrl |
| Nguồn được cấp | source.url, source.note |

visualType: NONE, BAR_CHART, LINE_GRAPH, PIE_CHART, TABLE, MIXED, PROCESS, MAP. Task 2 không gắn hình Task 1. Task 1 chính/overview/ngôn ngữ mô tả cần hình khi xuất bản; bản nháp cho phép chưa tải hình.

Vocabulary: term, meaning, collocation, example, level. level là mức từ tham khảo tùy chọn, không suy ra band IELTS; bản seed để trống. Khi xuất bản mỗi từ cần term/meaning/example.

Activity: id ổn định, type, stage, instruction, context, items[], example. type gồm SELECT_FEATURES, COMPARE_OVERVIEWS, PARAPHRASE, VOCAB_CONTEXT, LINK_IDEAS, REPAIR_SENTENCE, TRANSFER; stage gồm MODEL, GUIDED, INDEPENDENT, TRANSFER. example là ví dụ hướng dẫn được chủ ý cấp cho học viên.

SampleAnswer: activityId tham chiếu hoạt động hiện có, acceptedExamples[] và explanation. sampleAnswers không được trả trong AssignedDetail của học viên.

DTO server: WritingPayloads.java; validator: WritingContentHandler.java; kiểu frontend: features/writing/types.ts. File demo JSON bên dưới có ví dụ đầy đủ từng loại. Server từ chối trường lạ, sai kiểu/nhiệm vụ/nhãn, hình không thuộc người soạn hoặc chưa sẵn sàng.

## Seed và nguồn

Khi DEMO_ENABLED=true, DemoWritingInitializer tạo **7 bản nháp** thuộc tài khoản giảng viên demo: Task 1, Task 2 và năm bài bổ trợ. Không tự xuất bản/giao bài. Marker DEMO_WRITING_V1 trong source.note ngăn tạo lại mỗi lần restart; giữ marker nếu sửa bài mẫu để tiếp tục nhận diện seed.

- [JSON 7 học liệu](../source/server/src/main/resources/demo/writing-resources.json).
- [Biểu đồ tự dựng](../source/server/src/main/resources/demo/writing-transport.png).
- [CSV số liệu tự tạo](../statics/dataset/writing-transport-demo.csv).
- Task 1: Riverton, phương tiện đi làm 2010/2020, bài tham khảo 165 từ.
- Task 2: ưu tiên không gian xanh/giao thông công cộng, bài tham khảo 277 từ.

Đề, câu hỏi hướng dẫn, từ/câu và bài tham khảo được tự biên soạn cho demo; số liệu Riverton là giả lập. Chưa được chuyên gia xác nhận band, chưa coi như dữ liệu chấm AI. Các PDF trong plan là nguồn tham khảo, chưa tự tải/copy vào seed.

Nguồn cấu trúc được đối chiếu ngày 13/10/2026:

- [IELTS Academic Writing format](https://ielts.org/take-a-test/test-types/ielts-academic-test/ielts-academic-format-writing): Task 1/Task 2, yêu cầu từ và tiêu chí.
- [Writing band descriptors](https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf): rubric chính thức, bản cập nhật May 2023. Lưu URL tham khảo; chưa sao chép PDF hoặc lưu rubric band vào database.

## Demo qua giao diện

1. Chạy .\start.cmd ở thư mục project, mở http://localhost:5173, đăng nhập giảng viên.
2. Tạo lớp và cho tài khoản học viên tham gia.
3. Mở Bài Writing, xem trước một bản nháp mẫu hoặc tạo bài mới.
4. Task 1 tải hình PNG/JPEG; bổ trợ kiểm tra hoạt động và phương án riêng. Với sửa ngữ pháp, xác nhận câu lỗi/ngữ cảnh.
5. Lưu, xuất bản và giao cho lớp/nhóm được chọn.
6. Đăng nhập học viên trong trình duyệt/phiên riêng, mở Bài được giao.
7. Sửa bài đã xuất bản và xuất bản phiên bản mới: bài giao trước vẫn hiển thị bản cũ.

Hình tối đa 5 MiB, giới hạn 12 triệu pixel; kiểm tra định dạng thực bằng ImageIO. Tệp lưu trên asset_data, metadata/checksum ở MySQL. Tải ngoài quyền bị chặn; hỗ trợ byte Range. Nginx cho phép multipart tối đa 6 MiB.

## Kiểm thử và nối module sau

API test dùng MySQL Testcontainers riêng. Browser test tạo dữ liệu trong database nó đang trỏ tới, nên chạy trên bộ Docker demo/kiểm thử riêng. Sau browser test có thể chụp và đối chiếu dữ liệu qua restart:

    cd source/client
    node scripts/verify-writing-persistence.mjs capture
    # Ở terminal project: docker compose up -d --force-recreate --wait server client
    node scripts/verify-writing-persistence.mjs verify

Script dùng E2E_BASE_URL và các biến E2E_* giống bộ browser test; snapshot tạm nằm ở .test-state, không commit. Kiểm tra đủ bảy seed, phiên bản, bài giao/người nhận, nội dung học viên, checksum hình.

Bản hợp nhất giữ V2 nguyên checksum, đăng ký cả hai ContentHandler và dùng kiểu generic shared với DTO riêng của từng kỹ năng. imageAssets mặc định rỗng để handler không dùng hình tiếp tục hoạt động. Module 3 nhận AssignedDetail cố định resourceVersionId và lưu câu trả lời theo activity.id; module đánh giá phải kiểm tra thời điểm/policy trước cấp gợi ý riêng.

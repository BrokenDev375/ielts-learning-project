# HTTP API và message format — module 1 và module 2 Speaking/Writing

Base path /api, request/response JSON UTF-8. Các thao tác thành công dùng HTTP 200/201/204; lỗi 400/401/403/404/409/503/500. ID tài nguyên lấy từ response, không tin vai trò hoặc ID người dùng tự khai trong body.

## Session và CSRF

1. GET /api/auth/csrf trả {"token":"...","headerName":"X-CSRF-TOKEN"} và cookie session.
2. POST /api/auth/login gửi JSON email/password với token trong header.
3. Server xác thực, thay session ID và xóa token cũ. Client GET /api/auth/csrf trước mutation tiếp theo.
4. Trình duyệt tự gửi cookie JSESSIONID HttpOnly cùng origin; frontend không đọc/lưu password hoặc token session vào localStorage.
5. POST /api/auth/logout với CSRF token trả 204, xóa session/cookie.
6. Session timeout 8 giờ; backend restart yêu cầu đăng nhập lại. Dữ liệu lớp vẫn trong MySQL.
7. Lỗi CSRF_INVALID cho phép client lấy token mới và thử lại một lần, vì yêu cầu trước bị chặn trước xử lý nghiệp vụ.

## Các endpoint

| Method / path | Request | Response / quyền |
|---|---|---|
| GET /health | — | {"status":"UP"} nếu database sẵn sàng; public |
| GET /auth/csrf | — | token/headerName; public |
| POST /auth/register | fullName, email, password | UserView, 201; luôn tạo STUDENT, body có role bị từ chối |
| POST /auth/login | email, password | UserView, 200; sai thông tin trả 401 |
| GET /auth/me | — | UserView của session hiện tại |
| POST /auth/logout | — | 204, hết session |
| GET /classes | — | Lớp thuộc giảng viên hoặc lớp học viên đã tham gia |
| POST /classes | name, description tùy chọn, requestId UUID | TEACHER: 201 lớp mới; 200 phát lại cùng yêu cầu |
| GET /classes/{id} | — | Chủ lớp hoặc học viên thuộc lớp |
| PATCH /classes/{id} | name, description | Chỉ giảng viên sở hữu, 200 |
| POST /classes/join | code | STUDENT: {"classroom":{...},"alreadyJoined":false/true} |
| GET /classes/{id}/members | — | Chỉ chủ lớp, danh sách học viên |
| DELETE /classes/{id}/members/{studentId} | — | Chỉ chủ lớp, 204; gửi lại vẫn 204 |
| DELETE /classes/{id}/membership | — | Học viên rời lớp của chính mình, 204; gửi lại vẫn 204 |

## Ví dụ tạo lớp

Request (có cookie và X-CSRF-TOKEN):

    POST /api/classes
    Content-Type: application/json

    {
      "name": "IELTS Reading — Nhóm sáng",
      "description": "Lớp luyện đọc",
      "requestId": "f31c2186-cdf4-438d-b675-012c24d97c7e"
    }

ClassroomView:

    {
      "id": 1,
      "teacherId": 1,
      "teacherName": "Nguyễn Minh Anh",
      "name": "IELTS Reading — Nhóm sáng",
      "description": "Lớp luyện đọc",
      "joinCode": "ABCD2345",
      "memberCount": 0,
      "createdAt": "2026-10-11T00:00:00Z",
      "joinedAt": null
    }

Giá trị trên là **ví dụ format**, không phải dữ liệu hoặc kết quả đo. joinCode chỉ trả cho giảng viên sở hữu; response học viên có joinCode=null.

UserView gồm id, email, fullName, role, createdAt; không trả password/passwordHash. MemberView gồm id, fullName, email, joinedAt; chỉ chủ lớp được lấy roster.

## Yêu cầu trùng và đồng thời

Bản ORM dùng Spring Data JPA/Hibernate. Service tạo lớp khóa hàng tài khoản giảng viên; tham gia/xóa/rời lớp khóa hàng lớp. Các query đọc dùng JPQL projection để trả DTO. Unique/PK ở database vẫn bảo vệ trường hợp trùng; transaction lỗi được rollback. Không bắt lỗi constraint rồi tiếp tục dùng cùng persistence context bị lỗi. Các entity không được serialize trực tiếp ra response.


- Client sinh requestId UUID khi mở form tạo lớp, giữ mã đó khi thử lại sau lỗi/timeout.
- Unique(teacher_id, create_request_id) bảo đảm cùng yêu cầu không tạo hai lớp; cùng ID với nội dung khác trả 409 REQUEST_ID_REUSED.
- Nếu lớp đã được sửa, phát lại yêu cầu cũ với thông tin khác hiện tại sẽ trả conflict; mở form mới cho một lần tạo mới.
- join_code được SecureRandom sinh 8 ký tự, có unique constraint và thử sinh lại nếu đụng mã.
- Primary key(classroom_id, student_id) bảo vệ membership; gửi lại trả alreadyJoined=true.
- READ_COMMITTED cho transaction tạo/join giúp đọc dữ liệu đã commit khi có request trùng chạy đồng thời.
- Xóa/rời lớp kiểm tra vai trò/phạm vi và chỉ xóa liên kết membership, không xóa tài khoản.
- Request timeout không chứng minh server chưa lưu. Thử lại tạo lớp cùng requestId; tham gia/xóa/rời lớp có thể gửi lại an toàn.

## Error format

    {
      "code": "VALIDATION_ERROR",
      "message": "Vui lòng kiểm tra thông tin đã nhập.",
      "fieldErrors": { "name": "Nhập tên lớp." }
    }

400: JSON/field/ID/code sai; 401: chưa đăng nhập/sai mật khẩu; 403: CSRF hoặc trái quyền; 404: không có lớp/mã; 409: email/requestId trùng với nội dung không phù hợp; 503: database chưa sẵn sàng.

Không trả SQL, password hoặc stacktrace về client. Backend log phục vụ debug; frontend hiển thị thông báo dễ hiểu và giữ form chưa xác nhận. Connection HTTP/TCP được framework quản lý; đóng tab không xóa thành viên lớp.

## Học liệu và bài giao — V2

Mutation cần giảng viên và CSRF. Bản tích hợp đăng ký handler SPEAKING và WRITING; các kỹ năng chưa có handler trả SKILL_NOT_SUPPORTED.

| Method / path | Request | Response / quyền |
|---|---|---|
| GET /resources?skill=WRITING | — | Resource[] thuộc giảng viên, mỗi resource có version mới nhất |
| POST /resources | ResourceInput | 201 Resource, phiên bản 1 DRAFT |
| PUT /resources/{id}/draft | ResourceInput | 200 Resource; cập nhật nháp hoặc tạo nháp phiên bản mới từ bản đã xuất bản |
| GET /resources/{id}/versions | — | Version[] mới đến cũ; chỉ chủ sở hữu |
| POST /resources/{id}/versions/{versionId}/publish | — | 200 Version sau kiểm tra điều kiện xuất bản |
| POST /assets/images | multipart file | 201 Asset; PNG/JPEG tối đa 5 MiB, tối đa 12 triệu pixel |
| POST /assets | multipart file | 201 Asset audio; upload audio cho Speaking |
| GET /assets/{id} | tùy chọn Range | 200/206 byte tệp; chủ tệp hoặc người nhận bài đang có quyền |
| GET /assignments | — | Assignment[] của chủ lớp hoặc người nhận đang thuộc lớp |
| POST /assignments | Assign | 201 tạo mới; 200 phát lại requestId cùng nội dung |
| GET /assignments/{id} | — | AssignedDetail đúng phiên bản; teaching chỉ cho giảng viên, questions=[] cho Speaking/Writing |

Đường dẫn tính từ /api. Asset response: id, originalName, mimeType, sizeBytes, url. MIME kiểm tra từ tệp thực; SVG/tệp hỏng bị từ chối. Không lộ storageKey/đường dẫn trên máy chủ.

Resource/Version/AssignedDetail và payload Writing: [WRITING_MODULE_2.md](../../docs/WRITING_MODULE_2.md), [kiểu dùng chung](../client/src/features/learning/types.ts), [WritingPayloads](../server/src/main/java/vn/edu/ptit/ielts/dto/WritingPayloads.java). Response học viên loại toàn bộ teaching; content.activities[].example là ví dụ được chủ ý cấp trong bước hướng dẫn.

Assign:

    {
      "requestId": "c47bb745-c617-4d69-a4f0-945b008f0013",
      "classroomId": 1,
      "resourceVersionId": 12,
      "purpose": "PRACTICE",
      "recipientMode": "CLASS_SNAPSHOT",
      "studentIds": [],
      "opensAt": null,
      "dueAt": null,
      "allowLate": false,
      "maxAttempts": 1,
      "feedbackPolicy": "AFTER_SUBMISSION"
    }

ID minh họa format. SELECTED cần studentIds thuộc lớp; CLASS_SNAPSHOT chốt thành viên hiện tại, người tham gia sau không tự nhận bài. REMEDIAL dùng purpose=REMEDIAL; MAIN dùng PRACTICE/TEST_INITIAL/TEST_PERIODIC/RECHECK. Thời điểm ISO-8601 UTC, dueAt không trước opensAt.

Server khóa lớp khi giao; requestId duy nhất theo lớp chống ghi trùng. Phát lại cùng payload trả assignment cũ; khác payload trả 409 REQUEST_ID_REUSED. Frontend giữ requestId khi thử lại form chưa đổi nội dung. Nháp tài nguyên/upload chưa có idempotency key; không tự retry mutation khi timeout, ngoài trường hợp CSRF bị chặn trước xử lý.

opensAt được thực thi khi học viên tải bài/hình. dueAt/maxAttempts/allowLate/feedbackPolicy là cấu hình cho module 3; chưa có API bài làm/nộp hoặc mở phản hồi. Rời/xóa khỏi lớp chặn quyền hiện tại, giữ assignment_recipients. Bài đã giao giữ resourceVersionId khi xuất bản phiên bản mới.

Lỗi vẫn dùng ApiError; Writing thêm INVALID_WRITING, INVALID_IMAGE, IMAGE_TOO_LARGE (413). Nginx giới hạn multipart 6 MiB, Spring file 5 MiB. Không cấp hình qua URL static public.

Speaking schema: [SPEAKING_MODULE_2.md](../../docs/SPEAKING_MODULE_2.md), [kiểu frontend Speaking](../client/src/features/speaking/types.ts). Content có taskType/part/prompt/instructions/preparationSeconds/speakingSeconds/checklist/phonemes/items/assessmentCriteria; teaching có outlineExamples/teacherNotes chỉ dành cho giảng viên. Item có text/ipa/audioAssetId. GET /resources?skill=SPEAKING dùng cùng quyền và version flow; student GET /assignments trả cả hai kỹ năng được cấp, client chọn preview theo skill.

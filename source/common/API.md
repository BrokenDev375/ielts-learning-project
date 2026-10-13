# HTTP API và message format — module 1 và Speaking module 2

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


## Module 2 — tài liệu, phiên bản và giao bài Speaking

Tất cả endpoint cần session. Mutation cần CSRF; upload dùng multipart/form-data thay JSON. Chi tiết schema nội dung ở [Speaking module 2](../../docs/SPEAKING_MODULE_2.md).

| Method/path | Quyền và kết quả |
|---|---|
| GET /api/resources?skill=SPEAKING | Giảng viên: bài của mình, mỗi bài có version gần nhất |
| POST /api/resources | Giảng viên: ResourceInput; 201 Resource {id,skill,version} |
| GET /api/resources/{id} | Chủ bài: phiên bản gần nhất |
| GET /api/resources/{id}/versions | Chủ bài: Version[] mới trước |
| PUT /api/resources/{id}/draft | Chủ bài: ResourceInput; lưu nháp hoặc tạo nháp mới từ bài đã xuất bản |
| POST /api/resources/{id}/versions/{versionId}/publish | Chủ bài: kiểm tra rồi xuất bản; gọi lại phiên bản đã xuất bản giữ nguyên |
| POST /api/assets | Giảng viên: multipart file WAV/MP3/OGG tối đa 5 MiB; 201 {id,originalName,mimeType,sizeBytes,url} |
| GET /api/assets/{id} | Chủ audio hoặc học viên nhận bài đang mở/còn trong lớp; hỗ trợ Range 206 |
| POST /api/assignments | Chủ lớp và bài: Assign; 201 lần đầu, 200 phát lại cùng UUID/nội dung |
| GET /api/assignments | Giảng viên: bài đã giao; học viên: bài được nhận và còn thuộc lớp |
| GET /api/assignments/{id} | AssignedDetail theo format chung plan 6.4; học viên chỉ đọc đúng người nhận, sau thời gian mở |

ResourceInput gồm title, skill, resourceKind, activityType, topic, levelFramework (nullable), levelLabel, supportLevel, content, teaching, source. Version có thêm id/resourceId/versionNo/status/publishedAt. student DTO bỏ teaching, giữ content/source; không được dùng endpoint ngân hàng để lấy tài liệu ngoài assignment.

Assign body minh họa:

    {
      "requestId": "7f85f4ca-a0a2-40e3-9b0f-3934692a582f",
      "classroomId": 1,
      "resourceVersionId": 2,
      "purpose": "PRACTICE",
      "recipientMode": "CLASS_SNAPSHOT",
      "studentIds": [],
      "opensAt": null,
      "dueAt": "2026-10-30T16:00:00Z",
      "allowLate": false,
      "maxAttempts": 1,
      "feedbackPolicy": "TEACHER_RELEASE"
    }

ID và ngày trên là ví dụ format. SELECTED cần studentIds thuộc lớp. purpose của bài bổ trợ là REMEDIAL. Payload không chứa teacherId/studentId tự khai thay cho session.

AssignedDetail trả assignmentId, resourceId, resourceVersionId, skill, title, dueAt, content, questions (rỗng với Speaking); thêm metadata phiên bản và assignment cho thông tin lớp/điều kiện. Giữ format chung đã chốt tại plan 6.4. Teaching chỉ có trong response giảng viên.

AssignmentView gồm id, classroomId/classroomName, resourceVersionId, title, skill, purpose/status, opensAt/dueAt, allowLate, maxAttempts, feedbackPolicy, recipientCount, createdAt. Các mốc thời gian response là ISO-8601 UTC.

Lỗi bổ sung: INVALID_RESOURCE, INVALID_ASSET, INVALID_AUDIO (400); AUDIO_TOO_LARGE (413); RESOURCE_NOT_PUBLISHED/INVALID_RECIPIENTS/INVALID_DATES/INVALID_PURPOSE (400); ASSIGNMENT_NOT_OPEN/FORBIDDEN (403); REQUEST_ID_REUSED (409). Kỹ năng chưa có ResourceContentHandler trả SKILL_NOT_SUPPORTED (400).

Assignment giữ phiên bản và người nhận cố định, UUID/fingerprint chống ghi trùng. Ngân hàng tạo bài bằng POST hiện chưa có idempotency key; sau timeout nên tải lại để kiểm tra trước khi tạo lại. Endpoint Attempt/nháp/Submission/AI theo plan triển khai ở module sau.


CREATE TABLE assets (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính do server cấp',
    owner_id BIGINT NOT NULL COMMENT 'Người tải tệp',
    storage_key VARCHAR(255) NOT NULL COMMENT 'Đường dẫn nội bộ bất biến, không trả trực tiếp cho client',
    original_name VARCHAR(255) NOT NULL COMMENT 'Tên tệp hiển thị',
    mime_type VARCHAR(100) NOT NULL COMMENT 'Loại tệp đã kiểm tra',
    size_bytes BIGINT NOT NULL COMMENT 'Kích thước byte',
    sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Checksum nội dung',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'READY' COMMENT 'READY/RETIRED',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (id),
    CONSTRAINT uq_assets_storage UNIQUE (storage_key),
    CONSTRAINT fk_assets_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_assets_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_assets_status CHECK (status IN ('READY','RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Metadata tệp; file nằm trong kho tệp ngoài MySQL.';

CREATE TABLE learning_labels (
    code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Mã nhãn theo plan',
    skill VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'READING/LISTENING/SPEAKING/WRITING',
    display_name VARCHAR(150) NOT NULL COMMENT 'Tên tiếng Việt',
    label_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'TARGET hoặc REMEDIAL',
    description TEXT NOT NULL COMMENT 'Điều kiện áp dụng nhãn',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Nhãn được dùng cho bài mới',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (code),
    CONSTRAINT uq_learning_labels_skill UNIQUE (code, skill),
    CONSTRAINT ck_learning_labels_skill CHECK (skill IN ('READING','LISTENING','SPEAKING','WRITING')),
    CONSTRAINT ck_learning_labels_kind CHECK (label_kind IN ('TARGET','REMEDIAL')),
    CONSTRAINT ck_learning_labels_active CHECK (is_active IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Danh mục mục tiêu/nhóm bổ trợ đã thống nhất; trở ngại tự nhận xét lưu riêng.';

CREATE TABLE resources (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính do server cấp',
    owner_id BIGINT NOT NULL COMMENT 'Giảng viên sở hữu',
    skill VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'READING/LISTENING/SPEAKING/WRITING',
    archived_at DATETIME(6) NULL COMMENT 'Lưu trữ bài khỏi ngân hàng đang dùng',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (id),
    CONSTRAINT uq_resources_skill UNIQUE (id, skill),
    INDEX ix_resources_owner (owner_id, skill, archived_at),
    CONSTRAINT fk_resources_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_resources_skill CHECK (skill IN ('READING','LISTENING','SPEAKING','WRITING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Danh tính bài trong ngân hàng; nội dung nằm trong từng phiên bản.';

CREATE TABLE resource_versions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính do server cấp',
    resource_id BIGINT NOT NULL COMMENT 'Bài gốc',
    skill VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'READING/LISTENING/SPEAKING/WRITING',
    version_no INT NOT NULL COMMENT 'Số phiên bản từ 1',
    title VARCHAR(200) NOT NULL COMMENT 'Tên bài theo phiên bản',
    resource_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'MAIN/REMEDIAL',
    activity_type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'MCQ/DICTATION/WRITING_ESSAY/SPEAKING_RECORDING/OUTLINE/PRONUNCIATION/GUIDED',
    topic VARCHAR(150) NULL COMMENT 'Chủ đề',
    level_framework VARCHAR(24) NULL COMMENT 'CEFR/IELTS_PRACTICE/RAW',
    level_label VARCHAR(32) NULL COMMENT 'Mức luyện; không tự đổi sang band',
    support_level VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'INDEPENDENT' COMMENT 'MODEL/GUIDED/INDEPENDENT/TRANSFER',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ARCHIVED',
    content_schema_version SMALLINT NOT NULL DEFAULT 1 COMMENT 'Phiên bản định dạng content_json',
    content_json JSON NOT NULL COMMENT 'Nội dung khởi tạo được phép cấp: passage/paragraph, đề Writing/Speaking, bước tương tác',
    teaching_json JSON NULL COMMENT 'Hướng dẫn, phản hồi và nội dung mở có điều kiện; DTO học viên không trả nguyên khối này',
    source_json JSON NULL COMMENT 'URL/trang/tác giả, quyền sử dụng và trạng thái kiểm tra nguồn',
    rubric_version_id BIGINT NULL COMMENT 'Rubric đúng nhiệm vụ; thêm FK ở V4',
    reviewed_by BIGINT NULL COMMENT 'Giảng viên duyệt',
    reviewed_at DATETIME(6) NULL COMMENT 'Lúc duyệt',
    published_at DATETIME(6) NULL COMMENT 'Lúc xuất bản',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (id),
    CONSTRAINT uq_resource_versions_number UNIQUE (resource_id, version_no),
    CONSTRAINT uq_resource_versions_skill UNIQUE (id, skill),
    INDEX ix_resource_versions_find (skill, status, topic),
    CONSTRAINT fk_resource_versions_resource_skill FOREIGN KEY (resource_id, skill) REFERENCES resources (id, skill),
    CONSTRAINT fk_resource_versions_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_resource_versions_number CHECK (version_no > 0 AND content_schema_version > 0),
    CONSTRAINT ck_resource_versions_kind CHECK (resource_kind IN ('MAIN','REMEDIAL')),
    CONSTRAINT ck_resource_versions_activity CHECK (activity_type IN ('MCQ','DICTATION','WRITING_ESSAY','SPEAKING_RECORDING','OUTLINE','PRONUNCIATION','GUIDED')),
    CONSTRAINT ck_resource_versions_status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT ck_resource_versions_support CHECK (support_level IN ('MODEL','GUIDED','INDEPENDENT','TRANSFER')),
    CONSTRAINT ck_resource_versions_content CHECK (JSON_TYPE(content_json) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Snapshot nội dung đã xuất bản; sửa bài đã xuất bản bằng phiên bản mới.';

CREATE TABLE resource_assets (
    resource_version_id BIGINT NOT NULL COMMENT 'Phiên bản',
    asset_id BIGINT NOT NULL COMMENT 'Tệp',
    asset_role VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'VISUAL/AUDIO/PDF/MODEL_AUDIO/...',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Thứ tự hiển thị',
    PRIMARY KEY (resource_version_id, asset_id, asset_role),
    CONSTRAINT fk_resource_assets_version FOREIGN KEY (resource_version_id) REFERENCES resource_versions (id),
    CONSTRAINT fk_resource_assets_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT ck_resource_assets_sort CHECK (sort_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Các tệp đính kèm của đúng phiên bản bài.';

CREATE TABLE resource_labels (
    resource_version_id BIGINT NOT NULL COMMENT 'Phiên bản',
    skill VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'READING/LISTENING/SPEAKING/WRITING',
    label_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Nhãn đã duyệt',
    PRIMARY KEY (resource_version_id, label_code),
    INDEX ix_resource_labels_lookup (label_code, resource_version_id),
    CONSTRAINT fk_resource_labels_version_skill FOREIGN KEY (resource_version_id, skill) REFERENCES resource_versions (id, skill),
    CONSTRAINT fk_resource_labels_label_skill FOREIGN KEY (label_code, skill) REFERENCES learning_labels (code, skill)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Nhãn lọc bài/gợi ý bổ trợ; không suy ra điểm yếu chỉ từ nhãn.';

CREATE TABLE questions (
    resource_version_id BIGINT NOT NULL COMMENT 'Phiên bản bài',
    question_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Ánh xạ questionId',
    skill VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'READING/LISTENING/SPEAKING/WRITING',
    sort_order INT NOT NULL COMMENT 'Thứ tự câu',
    question_text TEXT NOT NULL COMMENT 'Nội dung câu',
    question_type VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'SINGLE_CHOICE' COMMENT 'Phạm vi hiện tại: chọn một đáp án',
    correct_option_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Ánh xạ correctOptionId; null khi đang soạn',
    explanation MEDIUMTEXT NULL COMMENT 'Lời giải mở trong review',
    evidence_json JSON NULL COMMENT 'paragraphId/câu hoặc segmentId/mốc nghe; service kiểm tra tham chiếu',
    primary_label_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Nhãn chính đã duyệt',
    secondary_labels_json JSON NULL COMMENT 'Nhãn phụ đã duyệt',
    label_status VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'UNDETERMINED' COMMENT 'UNDETERMINED/TEACHER_REVIEWED',
    PRIMARY KEY (resource_version_id, question_key),
    CONSTRAINT uq_questions_order UNIQUE (resource_version_id, sort_order),
    CONSTRAINT fk_questions_version_skill FOREIGN KEY (resource_version_id, skill) REFERENCES resource_versions (id, skill),
    CONSTRAINT fk_questions_label_skill FOREIGN KEY (primary_label_code, skill) REFERENCES learning_labels (code, skill),
    CONSTRAINT ck_questions_skill CHECK (skill IN ('READING','LISTENING')),
    CONSTRAINT ck_questions_type CHECK (question_type = 'SINGLE_CHOICE'),
    CONSTRAINT ck_questions_order CHECK (sort_order > 0),
    CONSTRAINT ck_questions_label_status CHECK (label_status IN ('UNDETERMINED','TEACHER_REVIEWED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Câu hỏi trắc nghiệm; q1 là khóa trong một phiên bản, không phải ID toàn hệ thống.';

CREATE TABLE question_options (
    resource_version_id BIGINT NOT NULL COMMENT 'Phiên bản',
    question_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Câu',
    option_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Ánh xạ optionId',
    option_text TEXT NOT NULL COMMENT 'Nội dung lựa chọn',
    sort_order INT NOT NULL COMMENT 'Thứ tự',
    PRIMARY KEY (resource_version_id, question_key, option_key),
    CONSTRAINT uq_question_options_order UNIQUE (resource_version_id, question_key, sort_order),
    CONSTRAINT fk_question_options_question FOREIGN KEY (resource_version_id, question_key) REFERENCES questions (resource_version_id, question_key),
    CONSTRAINT ck_question_options_order CHECK (sort_order > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Lựa chọn gắn đúng câu; không dùng vị trí A/B/C làm khóa thay đổi theo giao diện.';

CREATE TABLE listening_media (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính do server cấp',
    owner_id BIGINT NOT NULL COMMENT 'Người quản lý nguồn',
    source_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'YOUTUBE/AUDIO_ASSET',
    video_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'ID YouTube; không phải file upload',
    asset_id BIGINT NULL COMMENT 'File audio riêng',
    text_in_video VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'UNKNOWN' COMMENT 'BURNED_IN/NONE/UNKNOWN',
    source_json JSON NULL COMMENT 'URL/PDF đi kèm và trạng thái kiểm tra',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (id),
    CONSTRAINT fk_listening_media_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_listening_media_asset FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT ck_listening_media_source CHECK ((source_type = 'YOUTUBE' AND video_id IS NOT NULL AND asset_id IS NULL) OR (source_type = 'AUDIO_ASSET' AND asset_id IS NOT NULL AND video_id IS NULL)),
    CONSTRAINT ck_listening_media_text CHECK (text_in_video IN ('BURNED_IN','NONE','UNKNOWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Một nguồn nghe có thể dùng trong nhiều đoạn/bài; nguồn đã dùng được giữ bất biến.';

CREATE TABLE listening_transcript_versions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính do server cấp',
    media_id BIGINT NOT NULL COMMENT 'Nguồn nghe',
    start_seconds DECIMAL(10,3) NOT NULL COMMENT 'Mốc tuyệt đối đầu đoạn',
    end_seconds DECIMAL(10,3) NOT NULL COMMENT 'Mốc tuyệt đối cuối đoạn',
    revision_no INT NOT NULL COMMENT 'Lần sửa transcript của nguồn/khoảng nghe',
    transcript_text LONGTEXT NOT NULL COMMENT 'Lời tiếng Anh đã đối chiếu',
    comparison_rules_json JSON NULL COMMENT 'Quy ước số/tên/từ đệm/chữ hoa/dấu câu/biến thể',
    reviewed_by BIGINT NULL COMMENT 'Người duyệt',
    reviewed_at DATETIME(6) NULL COMMENT 'Lúc duyệt',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (id),
    CONSTRAINT uq_listening_transcript_versions_revision UNIQUE (media_id, start_seconds, end_seconds, revision_no),
    CONSTRAINT uq_listening_transcript_versions_binding UNIQUE (id, media_id, start_seconds, end_seconds),
    CONSTRAINT fk_listening_transcript_versions_media FOREIGN KEY (media_id) REFERENCES listening_media (id),
    CONSTRAINT fk_listening_transcript_versions_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_listening_transcript_versions_interval CHECK (start_seconds >= 0 AND end_seconds > start_seconds),
    CONSTRAINT ck_listening_transcript_versions_revision CHECK (revision_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Transcript bất biến cho đúng nguồn và khoảng nghe; sửa tạo revision mới.';

CREATE TABLE listening_segments (
    resource_version_id BIGINT NOT NULL COMMENT 'Phiên bản bài',
    segment_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'Ánh xạ segmentId',
    skill VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LISTENING' COMMENT 'Khóa ràng buộc kỹ năng',
    media_id BIGINT NOT NULL COMMENT 'Nguồn',
    title VARCHAR(200) NOT NULL COMMENT 'Tên đoạn',
    sort_order INT NOT NULL COMMENT 'Thứ tự',
    start_seconds DECIMAL(10,3) NOT NULL COMMENT 'Mốc đầu tuyệt đối',
    end_seconds DECIMAL(10,3) NOT NULL COMMENT 'Mốc cuối tuyệt đối',
    transcript_version_id BIGINT NULL COMMENT 'Transcript khớp nguồn/mốc; null khi soạn',
    PRIMARY KEY (resource_version_id, segment_key),
    CONSTRAINT uq_listening_segments_order UNIQUE (resource_version_id, sort_order),
    CONSTRAINT uq_listening_segments_transcript UNIQUE (resource_version_id, segment_key, transcript_version_id),
    CONSTRAINT fk_listening_segments_version_skill FOREIGN KEY (resource_version_id, skill) REFERENCES resource_versions (id, skill),
    CONSTRAINT fk_listening_segments_media FOREIGN KEY (media_id) REFERENCES listening_media (id),
    CONSTRAINT fk_listening_segments_transcript_binding FOREIGN KEY (transcript_version_id, media_id, start_seconds, end_seconds) REFERENCES listening_transcript_versions (id, media_id, start_seconds, end_seconds),
    CONSTRAINT ck_listening_segments_skill CHECK (skill = 'LISTENING'),
    CONSTRAINT ck_listening_segments_interval CHECK (start_seconds >= 0 AND end_seconds > start_seconds),
    CONSTRAINT ck_listening_segments_order CHECK (sort_order > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Đoạn nghe thuộc phiên bản bài, giữ transcript phù hợp đúng nguồn/mốc.';

CREATE TABLE assignments (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Khóa chính do server cấp',
    classroom_id BIGINT NOT NULL COMMENT 'Lớp',
    created_by BIGINT NOT NULL COMMENT 'Giảng viên tạo',
    resource_version_id BIGINT NOT NULL COMMENT 'Phiên bản cố định',
    purpose VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'TEST_INITIAL/TEST_PERIODIC/PRACTICE/REMEDIAL/RECHECK',
    recipient_mode VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'CLASS_SNAPSHOT/SELECTED',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CLOSED/ARCHIVED',
    opens_at DATETIME(6) NULL COMMENT 'Thời điểm mở',
    due_at DATETIME(6) NULL COMMENT 'Hạn nộp',
    allow_late BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Cho phép nộp muộn',
    max_attempts INT NOT NULL DEFAULT 1 COMMENT 'Số lần làm tối đa',
    feedback_policy VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'AFTER_SUBMISSION' COMMENT 'AFTER_SUBMISSION/AFTER_DUE/TEACHER_RELEASE',
    feedback_released_at DATETIME(6) NULL COMMENT 'Giảng viên mở phản hồi',
    settings_json JSON NULL COMMENT 'Các segment được giao, hỗ trợ được phép, điều kiện hoàn thành và cấu hình thưởng snapshot',
    create_request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'UUID giữ nguyên khi gửi lại cùng yêu cầu',
    request_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 của payload chuẩn hóa; server tính để nhận biết requestId bị dùng lại khác nội dung',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm cập nhật UTC',
    PRIMARY KEY (id),
    CONSTRAINT uq_assignments_version UNIQUE (id, resource_version_id),
    CONSTRAINT uq_assignments_request UNIQUE (created_by, create_request_id),
    INDEX ix_assignments_class (classroom_id, status, due_at),
    CONSTRAINT fk_assignments_class FOREIGN KEY (classroom_id) REFERENCES classrooms (id),
    CONSTRAINT fk_assignments_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_assignments_version FOREIGN KEY (resource_version_id) REFERENCES resource_versions (id),
    CONSTRAINT ck_assignments_purpose CHECK (purpose IN ('TEST_INITIAL','TEST_PERIODIC','PRACTICE','REMEDIAL','RECHECK')),
    CONSTRAINT ck_assignments_mode CHECK (recipient_mode IN ('CLASS_SNAPSHOT','SELECTED')),
    CONSTRAINT ck_assignments_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','ARCHIVED')),
    CONSTRAINT ck_assignments_dates CHECK (opens_at IS NULL OR due_at IS NULL OR due_at >= opens_at),
    CONSTRAINT ck_assignments_attempts CHECK (max_attempts > 0),
    CONSTRAINT ck_assignments_late CHECK (allow_late IN (0,1)),
    CONSTRAINT ck_assignments_feedback CHECK (feedback_policy IN ('AFTER_SUBMISSION','AFTER_DUE','TEACHER_RELEASE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Một lần giao một phiên bản bài cho một lớp hoặc tập thành viên trong lớp.';

CREATE TABLE assignment_recipients (
    assignment_id BIGINT NOT NULL COMMENT 'Bài được giao',
    student_id BIGINT NOT NULL COMMENT 'Người nhận',
    revoked_at DATETIME(6) NULL COMMENT 'Thu hồi quyền làm mới',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Thời điểm tạo UTC',
    PRIMARY KEY (assignment_id, student_id),
    INDEX ix_assignment_recipients_student (student_id, revoked_at, assignment_id),
    CONSTRAINT fk_assignment_recipients_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_assignment_recipients_student FOREIGN KEY (student_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Snapshot người nhận; không FK vào class_members để rời lớp không làm mất lịch sử.';

ALTER TABLE questions ADD CONSTRAINT fk_questions_correct_option FOREIGN KEY (resource_version_id, question_key, correct_option_key) REFERENCES question_options (resource_version_id, question_key, option_key);


-- Danh mục 16 nhãn hiện hành theo plan; không phải seed học liệu.
-- Không thêm hai nhãn Writing Task 2 còn đang đề xuất; không chốt cấu hình XP.
SET NAMES utf8mb4;
INSERT INTO learning_labels (code, skill, display_name, label_kind, description) VALUES
('READ_MAIN_IDEA', 'READING', 'Xác định ý chính', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('READ_DETAIL', 'READING', 'Tìm thông tin chi tiết', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('READ_CROSS_INFERENCE', 'READING', 'Suy luận liên đoạn', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('READ_CONTEXT_VOCAB', 'READING', 'Hiểu từ trong ngữ cảnh', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('LISTEN_DETAIL', 'LISTENING', 'Nghe thông tin chi tiết', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('LISTEN_CORRECTION', 'LISTENING', 'Nhận biết thông tin đính chính', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('LISTEN_PARAPHRASE', 'LISTENING', 'Hiểu diễn đạt tương đương', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('LISTEN_MAIN_IDEA', 'LISTENING', 'Hiểu ý chính', 'TARGET', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('LISTEN_SPEECH_COMPREHENSION', 'LISTENING', 'Nghe hiểu lời nói', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('SPEAK_DEVELOPMENT', 'SPEAKING', 'Phát triển câu trả lời', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('SPEAK_PRONUNCIATION', 'SPEAKING', 'Phát âm theo âm', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('OVERVIEW_SELECTION', 'WRITING', 'Chọn thông tin và viết overview', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('DESCRIPTIVE_LANGUAGE', 'WRITING', 'Từ vựng và cách diễn đạt mô tả', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('TOPIC_VOCABULARY', 'WRITING', 'Từ vựng theo chủ đề', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('IDEA_LINKING', 'WRITING', 'Liên kết ý', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.'),
('GRAMMAR_REPAIR', 'WRITING', 'Sửa lỗi ngữ pháp', 'REMEDIAL', 'Xem điều kiện áp dụng và bằng chứng trong PLAN_IELTS_LEARNING_SYSTEM.md.');

package vn.edu.ptit.ielts.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ResourceRequests {
    private ResourceRequests() {}
    public record ResourceInput(
        @NotBlank @Size(max=200) String title,
        @NotBlank @Pattern(regexp="READING|LISTENING|SPEAKING|WRITING") String skill,
        @NotBlank @Pattern(regexp="MAIN|REMEDIAL") String resourceKind,
        @NotBlank @Size(max=32) String activityType,
        @Size(max=150) String topic,
        @Pattern(regexp="CEFR|IELTS_PRACTICE|RAW") String levelFramework,
        @Size(max=32) String levelLabel,
        @NotBlank @Pattern(regexp="MODEL|GUIDED|INDEPENDENT|TRANSFER") String supportLevel,
        @NotNull JsonNode content, JsonNode teaching, JsonNode source) {}
    public record Assign(
        @NotNull UUID requestId, @Positive long classroomId, @Positive long resourceVersionId,
        @NotBlank @Pattern(regexp="TEST_INITIAL|TEST_PERIODIC|PRACTICE|REMEDIAL|RECHECK") String purpose,
        @NotBlank @Pattern(regexp="CLASS_SNAPSHOT|SELECTED") String recipientMode,
        @NotNull @Size(max=1000) List<@NotNull @Positive Long> studentIds,
        Instant opensAt, Instant dueAt, boolean allowLate,
        @Min(1) @Max(100) int maxAttempts,
        @NotBlank @Pattern(regexp="AFTER_SUBMISSION|AFTER_DUE|TEACHER_RELEASE") String feedbackPolicy) {}
}

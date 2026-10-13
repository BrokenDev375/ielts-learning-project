package vn.edu.ptit.ielts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public final class LearningViews {
    private LearningViews() {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Version(long id,long resourceId,int versionNo,String title,String skill,String resourceKind,
        String activityType,String topic,String levelFramework,String levelLabel,String supportLevel,String status,
        JsonNode content,JsonNode teaching,JsonNode source,Instant publishedAt) {
        public ResourceRequests.ResourceInput input() { return new ResourceRequests.ResourceInput(title,skill,resourceKind,activityType,topic,levelFramework,levelLabel,supportLevel,content,teaching,source); }
        public Version student() { return new Version(id,resourceId,versionNo,title,skill,resourceKind,activityType,topic,levelFramework,levelLabel,supportLevel,status,content,null,source,publishedAt); }
    }
    public record Resource(long id,String skill,Version version) {}
    public record Assignment(long id,long classroomId,String classroomName,long resourceVersionId,String title,
        String skill,String purpose,String status,Instant opensAt,Instant dueAt,boolean allowLate,int maxAttempts,
        String feedbackPolicy,long recipientCount,Instant createdAt) {}
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Assigned(long assignmentId,long resourceId,long resourceVersionId,String skill,String title,
        Instant dueAt,JsonNode content,java.util.List<Object> questions,String resourceKind,String activityType,
        String topic,String levelFramework,String levelLabel,String supportLevel,int versionNo,String status,
        JsonNode teaching,JsonNode source,Instant publishedAt,Assignment assignment) {
        public static Assigned from(Assignment a,Version v) {
            return new Assigned(a.id(),v.resourceId(),v.id(),v.skill(),v.title(),a.dueAt(),v.content(),java.util.List.of(),
                v.resourceKind(),v.activityType(),v.topic(),v.levelFramework(),v.levelLabel(),v.supportLevel(),
                v.versionNo(),v.status(),v.teaching(),v.source(),v.publishedAt(),a);
        }
    }
    public record Asset(long id,String originalName,String mimeType,long sizeBytes,String url) {}
    public record CreatedAssignment(Assignment assignment,boolean created) {}
}

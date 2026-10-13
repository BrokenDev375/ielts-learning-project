package vn.edu.ptit.ielts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.ptit.ielts.dto.ResourceRequests;

@Entity @Table(name="assignments")
public class AssignmentEntity extends StoredEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="classroom_id",nullable=false) private ClassroomEntity classroom;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by",nullable=false) private UserEntity createdBy;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="resource_version_id",nullable=false) private ResourceVersionEntity resourceVersion;
    @Column(name="purpose",length=24,nullable=false) private String purpose;
    @Column(name="recipient_mode",length=24,nullable=false) private String recipientMode;
    @Column(name="status",length=16,nullable=false) private String status="PUBLISHED";
    @Column(name="opens_at") private Instant opensAt;
    @Column(name="due_at") private Instant dueAt;
    @Column(name="allow_late",nullable=false) private boolean allowLate;
    @Column(name="max_attempts",nullable=false) private int maxAttempts;
    @Column(name="feedback_policy",length=24,nullable=false) private String feedbackPolicy;
    @Column(name="feedback_released_at") private Instant feedbackReleasedAt;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="settings_json",columnDefinition="json") private JsonNode settings;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name="create_request_id",length=36,nullable=false,columnDefinition="char(36)") private String createRequestId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name="request_fingerprint",length=64,nullable=false,columnDefinition="char(64)") private String requestFingerprint;
    @org.hibernate.annotations.UpdateTimestamp(source=org.hibernate.annotations.SourceType.DB) @Column(name="updated_at",nullable=false,columnDefinition="datetime(6)") private Instant updatedAt;
    protected AssignmentEntity(){}
    public ClassroomEntity getClassroom(){return classroom;}
    public void setClassroom(ClassroomEntity value){this.classroom=value;}
    public UserEntity getCreatedBy(){return createdBy;}
    public void setCreatedBy(UserEntity value){this.createdBy=value;}
    public ResourceVersionEntity getResourceVersion(){return resourceVersion;}
    public void setResourceVersion(ResourceVersionEntity value){this.resourceVersion=value;}
    public String getPurpose(){return purpose;}
    public void setPurpose(String value){this.purpose=value;}
    public String getRecipientMode(){return recipientMode;}
    public void setRecipientMode(String value){this.recipientMode=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){this.status=value;}
    public Instant getOpensAt(){return opensAt;}
    public void setOpensAt(Instant value){this.opensAt=value;}
    public Instant getDueAt(){return dueAt;}
    public void setDueAt(Instant value){this.dueAt=value;}
    public boolean getAllowLate(){return allowLate;}
    public void setAllowLate(boolean value){this.allowLate=value;}
    public int getMaxAttempts(){return maxAttempts;}
    public void setMaxAttempts(int value){this.maxAttempts=value;}
    public String getFeedbackPolicy(){return feedbackPolicy;}
    public void setFeedbackPolicy(String value){this.feedbackPolicy=value;}
    public Instant getFeedbackReleasedAt(){return feedbackReleasedAt;}
    public void setFeedbackReleasedAt(Instant value){this.feedbackReleasedAt=value;}
    public JsonNode getSettings(){return settings;}
    public void setSettings(JsonNode value){this.settings=value;}
    public String getCreateRequestId(){return createRequestId;}
    public void setCreateRequestId(String value){this.createRequestId=value;}
    public String getRequestFingerprint(){return requestFingerprint;}
    public void setRequestFingerprint(String value){this.requestFingerprint=value;}
    public Instant getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(Instant value){this.updatedAt=value;}
    public AssignmentEntity(ClassroomEntity room,UserEntity teacher,ResourceVersionEntity version,ResourceRequests.Assign i,String fingerprint) {
        classroom=room;createdBy=teacher;resourceVersion=version;purpose=i.purpose();recipientMode=i.recipientMode();
        opensAt=i.opensAt();dueAt=i.dueAt();allowLate=i.allowLate();maxAttempts=i.maxAttempts();feedbackPolicy=i.feedbackPolicy();
        createRequestId=i.requestId().toString();requestFingerprint=fingerprint;
    }
}

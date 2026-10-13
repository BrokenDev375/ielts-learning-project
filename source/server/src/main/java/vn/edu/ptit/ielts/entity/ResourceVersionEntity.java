package vn.edu.ptit.ielts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.dto.ResourceRequests;

@Entity @Table(name="resource_versions")
public class ResourceVersionEntity extends StoredEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="resource_id",nullable=false) private ResourceEntity resource;
    @Column(name="skill",length=16,nullable=false) private String skill;
    @Column(name="version_no",nullable=false) private int versionNo;
    @Column(name="title",length=200,nullable=false) private String title;
    @Column(name="resource_kind",length=16,nullable=false) private String resourceKind;
    @Column(name="activity_type",length=32,nullable=false) private String activityType;
    @Column(name="topic",length=150) private String topic;
    @Column(name="level_framework",length=24) private String levelFramework;
    @Column(name="level_label",length=32) private String levelLabel;
    @Column(name="support_level",length=16,nullable=false) private String supportLevel;
    @Column(name="status",length=16,nullable=false) private String status="DRAFT";
    @Column(name="content_schema_version",nullable=false) private short contentSchemaVersion=1;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="content_json",columnDefinition="json") private JsonNode content;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="teaching_json",columnDefinition="json") private JsonNode teaching;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="source_json",columnDefinition="json") private JsonNode source;
    @Column(name="rubric_version_id") private Long rubricVersionId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private UserEntity reviewer;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @Column(name="published_at") private Instant publishedAt;
    protected ResourceVersionEntity(){}
    public ResourceEntity getResource(){return resource;}
    public void setResource(ResourceEntity value){this.resource=value;}
    public String getSkill(){return skill;}
    public void setSkill(String value){this.skill=value;}
    public int getVersionNo(){return versionNo;}
    public void setVersionNo(int value){this.versionNo=value;}
    public String getTitle(){return title;}
    public void setTitle(String value){this.title=value;}
    public String getResourceKind(){return resourceKind;}
    public void setResourceKind(String value){this.resourceKind=value;}
    public String getActivityType(){return activityType;}
    public void setActivityType(String value){this.activityType=value;}
    public String getTopic(){return topic;}
    public void setTopic(String value){this.topic=value;}
    public String getLevelFramework(){return levelFramework;}
    public void setLevelFramework(String value){this.levelFramework=value;}
    public String getLevelLabel(){return levelLabel;}
    public void setLevelLabel(String value){this.levelLabel=value;}
    public String getSupportLevel(){return supportLevel;}
    public void setSupportLevel(String value){this.supportLevel=value;}
    public String getStatus(){return status;}
    public void setStatus(String value){this.status=value;}
    public short getContentSchemaVersion(){return contentSchemaVersion;}
    public void setContentSchemaVersion(short value){this.contentSchemaVersion=value;}
    public JsonNode getContent(){return content;}
    public void setContent(JsonNode value){this.content=value;}
    public JsonNode getTeaching(){return teaching;}
    public void setTeaching(JsonNode value){this.teaching=value;}
    public JsonNode getSource(){return source;}
    public void setSource(JsonNode value){this.source=value;}
    public Long getRubricVersionId(){return rubricVersionId;}
    public void setRubricVersionId(Long value){this.rubricVersionId=value;}
    public UserEntity getReviewer(){return reviewer;}
    public void setReviewer(UserEntity value){this.reviewer=value;}
    public Instant getReviewedAt(){return reviewedAt;}
    public void setReviewedAt(Instant value){this.reviewedAt=value;}
    public Instant getPublishedAt(){return publishedAt;}
    public void setPublishedAt(Instant value){this.publishedAt=value;}
    public ResourceVersionEntity(ResourceEntity resource,int no,ResourceRequests.ResourceInput input) {
        this.resource=resource;this.versionNo=no;this.skill=input.skill();update(input);
    }
    public void update(ResourceRequests.ResourceInput input) {
        title=input.title().strip();resourceKind=input.resourceKind();activityType=input.activityType();topic=input.topic();
        levelFramework=input.levelFramework();levelLabel=input.levelLabel();supportLevel=input.supportLevel();
        content=input.content().deepCopy();teaching=input.teaching()==null?null:input.teaching().deepCopy();
        source=input.source()==null?null:input.source().deepCopy();
    }
    public LearningViews.Version view() {
        return new LearningViews.Version(getId(),resource.getId(),versionNo,title,skill,resourceKind,activityType,
            topic,levelFramework,levelLabel,supportLevel,status,content.deepCopy(),
            teaching==null?null:teaching.deepCopy(),source==null?null:source.deepCopy(),publishedAt);
    }
}

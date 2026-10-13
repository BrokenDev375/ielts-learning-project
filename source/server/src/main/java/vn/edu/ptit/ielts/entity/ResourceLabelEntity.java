package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
@Entity @Table(name="resource_labels")
public class ResourceLabelEntity {
    @EmbeddedId private ResourceLabelKey id;
    @MapsId("resourceVersionId") @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="resource_version_id") private ResourceVersionEntity version;
    @MapsId("labelCode") @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="label_code") private LearningLabelEntity label;
    @Column(nullable=false,length=16) private String skill;
    protected ResourceLabelEntity(){}
    public ResourceLabelEntity(ResourceVersionEntity version,LearningLabelEntity label,String skill){this.version=version;this.label=label;this.skill=skill;id=new ResourceLabelKey(version.getId(),label.getCode());}
}

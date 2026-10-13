package vn.edu.ptit.ielts.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="resources")
public class ResourceEntity extends StoredEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false) private UserEntity owner;
    @Column(name="skill",length=16,nullable=false) private String skill;
    @Column(name="archived_at") private Instant archivedAt;
    protected ResourceEntity(){}
    public UserEntity getOwner(){return owner;}
    public void setOwner(UserEntity value){this.owner=value;}
    public String getSkill(){return skill;}
    public void setSkill(String value){this.skill=value;}
    public Instant getArchivedAt(){return archivedAt;}
    public void setArchivedAt(Instant value){this.archivedAt=value;}
    public ResourceEntity(UserEntity owner,String skill){this.owner=owner;this.skill=skill;}
}

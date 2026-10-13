package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
@Entity @Table(name="learning_labels")
public class LearningLabelEntity {
    @Id @Column(length=64) private String code;
    @Column(nullable=false,length=16) private String skill;
    @Column(name="display_name",nullable=false,length=150) private String displayName;
    @Column(name="label_kind",nullable=false,length=16) private String labelKind;
    @Column(nullable=false,columnDefinition="text") private String description;
    @Column(name="is_active",nullable=false) private boolean active;
    protected LearningLabelEntity(){}
    public String getCode(){return code;}
    public String getSkill(){return skill;}
}

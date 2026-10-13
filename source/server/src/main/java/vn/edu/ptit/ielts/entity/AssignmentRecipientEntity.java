package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
@Entity @Table(name="assignment_recipients")
public class AssignmentRecipientEntity {
    @EmbeddedId private AssignmentRecipientKey id;
    @MapsId("assignmentId") @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignment_id") private AssignmentEntity assignment;
    @MapsId("studentId") @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="student_id") private UserEntity student;
    @Column(name="revoked_at") private Instant revokedAt;
    @CreationTimestamp(source=SourceType.DB) @Column(name="created_at",nullable=false,updatable=false,columnDefinition="datetime(6)") private Instant createdAt;
    protected AssignmentRecipientEntity(){}
    public AssignmentRecipientEntity(AssignmentEntity a,UserEntity s){assignment=a;student=s;id=new AssignmentRecipientKey(a.getId(),s.getId());}
}

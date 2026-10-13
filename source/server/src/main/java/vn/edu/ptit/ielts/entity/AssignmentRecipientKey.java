package vn.edu.ptit.ielts.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
@Embeddable
public class AssignmentRecipientKey implements Serializable {
    @Column(name="assignment_id") private Long assignmentId;
    @Column(name="student_id") private Long studentId;
    protected AssignmentRecipientKey(){}
    public AssignmentRecipientKey(Long assignmentId,Long studentId){this.assignmentId=assignmentId;this.studentId=studentId;}
    public Long getAssignmentId(){return assignmentId;}
    public Long getStudentId(){return studentId;}
    @Override public boolean equals(Object o){return o instanceof AssignmentRecipientKey k && Objects.equals(assignmentId,k.assignmentId) && Objects.equals(studentId,k.studentId);}
    @Override public int hashCode(){return Objects.hash(assignmentId,studentId);}
}

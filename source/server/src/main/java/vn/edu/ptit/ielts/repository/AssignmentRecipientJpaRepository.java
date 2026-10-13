package vn.edu.ptit.ielts.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.ielts.entity.AssignmentRecipientEntity;
import vn.edu.ptit.ielts.entity.AssignmentRecipientKey;

public interface AssignmentRecipientJpaRepository extends JpaRepository<AssignmentRecipientEntity,AssignmentRecipientKey> {
    long countByAssignmentId(long assignmentId);
}

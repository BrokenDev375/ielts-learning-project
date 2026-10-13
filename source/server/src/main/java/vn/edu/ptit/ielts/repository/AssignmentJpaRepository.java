package vn.edu.ptit.ielts.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.dto.LearningViews;
import vn.edu.ptit.ielts.entity.AssignmentEntity;

public interface AssignmentJpaRepository extends JpaRepository<AssignmentEntity,Long> {
    String VIEW="""
        select new vn.edu.ptit.ielts.dto.LearningViews$Assignment(a.id,a.classroom.id,a.classroom.name,
        a.resourceVersion.id,a.resourceVersion.title,a.resourceVersion.skill,a.purpose,a.status,
        a.opensAt,a.dueAt,a.allowLate,a.maxAttempts,a.feedbackPolicy,
        (select count(r) from AssignmentRecipientEntity r where r.assignment.id=a.id and r.revokedAt is null),a.createdAt)
        from AssignmentEntity a
        """;
    String RECIPIENT="""
        a.status in ('PUBLISHED','CLOSED') and
        exists(select r from AssignmentRecipientEntity r where r.assignment.id=a.id and r.student.id=:actor and r.revokedAt is null)
        and exists(select m from ClassMemberEntity m where m.classroom.id=a.classroom.id and m.student.id=:actor)
        """;
    @Query(VIEW+" where a.createdBy.id=:actor order by a.createdAt desc,a.id desc")
    List<LearningViews.Assignment> teacherList(@Param("actor") long actor);
    @Query(VIEW+" where "+RECIPIENT+" order by a.createdAt desc,a.id desc")
    List<LearningViews.Assignment> studentList(@Param("actor") long actor);
    @Query(VIEW+" where a.id=:id")
    Optional<LearningViews.Assignment> viewById(@Param("id") long id);
    Optional<AssignmentEntity> findByCreatedByIdAndCreateRequestId(long owner,String requestId);
    boolean existsByIdAndCreatedById(long id,long teacherId);
    @Query("select count(a) from AssignmentEntity a where a.id=:id and "+RECIPIENT)
    long visibleToStudent(@Param("id") long id,@Param("actor") long studentId);
}

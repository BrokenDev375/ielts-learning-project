package vn.edu.ptit.ielts.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.dto.MemberView;
import vn.edu.ptit.ielts.entity.ClassMemberEntity;
import vn.edu.ptit.ielts.entity.ClassMemberKey;

public interface ClassMemberJpaRepository extends JpaRepository<ClassMemberEntity,ClassMemberKey> {
    @Query("select new vn.edu.ptit.ielts.dto.MemberView(m.student.id,m.student.fullName,m.student.email,m.joinedAt) from ClassMemberEntity m where m.classroom.id=:id order by m.joinedAt,m.student.id")
    List<MemberView> members(@Param("id") long classroomId);
    long countByClassroomId(long classroomId);
    @Query("select m.student.id from ClassMemberEntity m where m.classroom.id=:id order by m.student.id")
    List<Long> studentIds(@Param("id") long classroomId);
}

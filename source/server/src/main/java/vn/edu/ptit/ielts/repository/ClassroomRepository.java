package vn.edu.ptit.ielts.repository;

import java.util.List;
import java.util.Optional;
import vn.edu.ptit.ielts.dto.ClassroomRecord;
import vn.edu.ptit.ielts.dto.MemberView;
import vn.edu.ptit.ielts.entity.ClassroomEntity;
import vn.edu.ptit.ielts.enums.Role;

/** Contract injected by callers; implementation is in repository.impl. */
public interface ClassroomRepository {
    List<ClassroomRecord> list(long actor, Role role);
    Optional<ClassroomRecord> findById(long id, long actor);
    Optional<ClassroomRecord> findByCode(String code, long actor);
    Optional<ClassroomRecord> findByRequest(long teacher, String request);
    boolean codeExists(String code);
    void lockTeacher(long id);
    Optional<ClassroomEntity> lock(long id);
    long create(long teacher, String name, String description, String code, String request);
    void edit(long id, String name, String description);
    List<MemberView> members(long id);
    boolean isMember(long classroom, long student);
    void addMember(long classroom, long student);
    int removeMember(long classroom, long student);
}

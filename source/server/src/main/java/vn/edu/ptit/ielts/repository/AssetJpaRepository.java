package vn.edu.ptit.ielts.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.entity.AssetEntity;

public interface AssetJpaRepository extends JpaRepository<AssetEntity,Long> {
    boolean existsByIdAndOwnerIdAndStatusAndMimeTypeStartingWith(long id,long owner,String status,String prefix);
    boolean existsByIdAndOwnerIdAndStatus(long id,long owner,String status);
    @Query("""
        select count(ra) from ResourceAssetEntity ra
        join AssignmentEntity a on a.resourceVersion.id=ra.version.id
        join AssignmentRecipientEntity ar on ar.assignment.id=a.id
        join ClassMemberEntity cm on cm.classroom.id=a.classroom.id and cm.student.id=ar.student.id
        where ra.asset.id=:id and ar.student.id=:student and ar.revokedAt is null and ra.asset.status='READY'
        and a.status in ('PUBLISHED','CLOSED') and (a.opensAt is null or a.opensAt<=:now)
        """)
    long visibleToStudent(@Param("id") long id,@Param("student") long student,@Param("now") java.time.Instant now);
}

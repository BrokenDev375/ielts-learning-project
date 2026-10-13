package vn.edu.ptit.ielts.repository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.entity.ResourceEntity;

public interface ResourceJpaRepository extends JpaRepository<ResourceEntity,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResourceEntity r where r.id=:id and r.archivedAt is null")
    Optional<ResourceEntity> lockById(@Param("id") long id);
    Optional<ResourceEntity> findByIdAndArchivedAtIsNull(long id);
}

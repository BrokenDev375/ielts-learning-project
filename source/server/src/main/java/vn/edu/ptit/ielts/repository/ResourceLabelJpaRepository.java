package vn.edu.ptit.ielts.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.entity.ResourceLabelEntity;
import vn.edu.ptit.ielts.entity.ResourceLabelKey;

public interface ResourceLabelJpaRepository extends JpaRepository<ResourceLabelEntity,ResourceLabelKey> {
    @Modifying(flushAutomatically=true)
    @Query("delete from ResourceLabelEntity r where r.version.id=:id")
    void deleteForVersion(@Param("id") long id);
}

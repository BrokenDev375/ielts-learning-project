package vn.edu.ptit.ielts.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.entity.ResourceAssetEntity;
import vn.edu.ptit.ielts.entity.ResourceAssetKey;

public interface ResourceAssetJpaRepository extends JpaRepository<ResourceAssetEntity,ResourceAssetKey> {
    @Modifying(flushAutomatically=true)
    @Query("delete from ResourceAssetEntity r where r.version.id=:id")
    void deleteForVersion(@Param("id") long id);
}

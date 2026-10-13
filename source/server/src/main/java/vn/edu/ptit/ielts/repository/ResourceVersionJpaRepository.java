package vn.edu.ptit.ielts.repository;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.edu.ptit.ielts.entity.ResourceVersionEntity;

public interface ResourceVersionJpaRepository extends JpaRepository<ResourceVersionEntity,Long> {
    List<ResourceVersionEntity> findByResourceIdOrderByVersionNoDesc(long resourceId);
    @Query("""
        select v from ResourceVersionEntity v where v.resource.owner.id=:owner and v.resource.archivedAt is null
        and (:skill is null or v.skill=:skill)
        and v.versionNo=(select max(v2.versionNo) from ResourceVersionEntity v2 where v2.resource.id=v.resource.id)
        order by v.resource.id desc
        """)
    List<ResourceVersionEntity> latest(@Param("owner") long owner,@Param("skill") String skill);
    @Query("select v from ResourceVersionEntity v where v.resource.owner.id=:owner and v.skill='SPEAKING' and v.activityType=:activity")
    List<ResourceVersionEntity> seedCandidates(@Param("owner") long owner,@Param("activity") String activity);
    @Query("select v from ResourceVersionEntity v where v.resource.owner.id=:owner and v.skill=:skill")
    List<ResourceVersionEntity> seedCandidatesForSkill(@Param("owner") long owner,@Param("skill") String skill);
}

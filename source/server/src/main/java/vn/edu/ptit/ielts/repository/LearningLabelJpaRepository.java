package vn.edu.ptit.ielts.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.ptit.ielts.entity.LearningLabelEntity;

public interface LearningLabelJpaRepository extends JpaRepository<LearningLabelEntity,String>{}

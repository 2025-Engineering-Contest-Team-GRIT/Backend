package grit.guidance.domain.simulation.repository;

import grit.guidance.domain.simulation.entity.GraduationPlanCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraduationPlanCourseRepository extends JpaRepository<GraduationPlanCourse, Long> {
    List<GraduationPlanCourse> findByGraduationPlanId(Long graduationPlanId);
}

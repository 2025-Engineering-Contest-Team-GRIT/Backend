package grit.guidance.domain.graduation.repository;

import grit.guidance.domain.graduation.entity.GraduationPlanCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationPlanCourseRepository extends JpaRepository<GraduationPlanCourse, Long> {
}
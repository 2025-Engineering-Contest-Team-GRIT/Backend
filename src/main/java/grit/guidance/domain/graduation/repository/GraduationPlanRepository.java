package grit.guidance.domain.graduation.repository;

import grit.guidance.domain.graduation.entity.GraduationPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraduationPlanRepository extends JpaRepository<GraduationPlan, Long> {
}
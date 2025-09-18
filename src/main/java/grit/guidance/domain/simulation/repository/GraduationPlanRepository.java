package grit.guidance.domain.simulation.repository;

import grit.guidance.domain.simulation.entity.GraduationPlan;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraduationPlanRepository extends JpaRepository<GraduationPlan, Long> {
    List<GraduationPlan> findByUsers(Users users);
}

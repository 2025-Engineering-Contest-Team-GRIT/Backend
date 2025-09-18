package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.GraduationRequirement;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GraduationRequirementRepository extends JpaRepository<GraduationRequirement, Long> {
    Optional<GraduationRequirement> findByUsers(Users users);
}
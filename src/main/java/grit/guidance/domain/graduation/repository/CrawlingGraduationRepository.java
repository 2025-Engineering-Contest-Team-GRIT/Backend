package grit.guidance.domain.graduation.repository;

import grit.guidance.domain.graduation.entity.CrawlingGraduation;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CrawlingGraduationRepository extends JpaRepository<CrawlingGraduation, Long> {
    Optional<CrawlingGraduation> findByUsers(Users users);
}
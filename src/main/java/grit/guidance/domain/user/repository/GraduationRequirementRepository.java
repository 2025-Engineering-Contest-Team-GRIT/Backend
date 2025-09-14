package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.GraduationRequirement;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GraduationRequirementRepository extends JpaRepository<GraduationRequirement, Long> {

    // Users 엔티티를 기준으로 졸업 요건 정보를 찾는 메서드입니다.
    // Optional은 결과가 없을 경우를 대비해 NullPointerException을 방지합니다.
    Optional<GraduationRequirement> findByUsers(Users users);
}
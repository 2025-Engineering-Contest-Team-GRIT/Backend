package grit.guidance.global.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemDataRepository extends JpaRepository<SystemData, Long> {
    // SystemData는 단일 레코드만 유지하므로 기본 메서드들만 사용
}

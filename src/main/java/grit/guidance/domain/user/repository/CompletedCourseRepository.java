package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompletedCourseRepository extends JpaRepository<CompletedCourse, Long> {
    void deleteByUsers(Users user);

    // 특정 사용자의 모든 이수 과목 정보를 조회하는 메서드
    List<CompletedCourse> findByUsers(Users user);
}
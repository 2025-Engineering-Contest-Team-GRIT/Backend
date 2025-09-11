package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompletedCourseRepository extends JpaRepository<CompletedCourse, Long> {
    void deleteByUsers(Users user);
}
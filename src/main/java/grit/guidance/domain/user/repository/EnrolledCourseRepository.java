package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.EnrolledCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrolledCourseRepository extends JpaRepository<EnrolledCourse, Long> {
    void deleteByUser(Users user);
}
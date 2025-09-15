package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    Optional<Course> findByCourseName(String courseName);
}
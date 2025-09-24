package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    Optional<Course> findByCourseName(String courseName);
    List<Course> findAllByCourseCodeIn(List<String> courseCodes);

    @Modifying
    @Query("UPDATE Course c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.deletedAt IS NULL")
    void deleteAllSoft();
    
}
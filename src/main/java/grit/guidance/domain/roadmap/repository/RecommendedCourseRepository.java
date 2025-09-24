package grit.guidance.domain.roadmap.repository;

import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.roadmap.entity.RecommendedCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendedCourseRepository extends JpaRepository<RecommendedCourse, Long> {
    List<RecommendedCourse> findAllByUser(Users user);
    void deleteByUser(Users user);
    List<RecommendedCourse> findByUserAndRecommendGradeAndRecommendSemester(Users user, Integer recommendGrade, Semester recommendSemester);
}
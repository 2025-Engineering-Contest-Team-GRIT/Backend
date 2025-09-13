package grit.guidance.domain.user.repository;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompletedCourseRepository extends JpaRepository<CompletedCourse, Long> {
    @Modifying
    @Query("DELETE FROM CompletedCourse cc WHERE cc.users = :user")
    void deleteByUsers(@Param("user") Users user);
    
    // 중복 체크: 사용자, 과목, 연도, 학기가 모두 같은 완료 과목이 있는지 확인
    boolean existsByUsersAndCourseAndCompletedYearAndCompletedSemester(
        Users user, Course course, int completedYear, Semester completedSemester);
}
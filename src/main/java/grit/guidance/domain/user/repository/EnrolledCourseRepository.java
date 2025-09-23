package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.EnrolledCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrolledCourseRepository extends JpaRepository<EnrolledCourse, Long> {
    void deleteByUser(Users user);
    
    // 사용자로 수강중인 과목 목록 조회
    List<EnrolledCourse> findByUser(Users user);
    
    // 사용자 ID로 수강중인 과목 ID 목록 조회
    @Query("SELECT ec.course.id FROM EnrolledCourse ec WHERE ec.user.id = :userId")
    List<Long> findCourseIdsByUserId(@Param("userId") Long userId);
    
    // 학번으로 수강중인 과목 조회 (Course 정보 포함)
    @Query("SELECT ec FROM EnrolledCourse ec JOIN FETCH ec.course WHERE ec.user.studentId = :studentId")
    List<EnrolledCourse> findByUsersStudentId(@Param("studentId") String studentId);
}
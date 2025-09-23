package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.CoursePrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {
    
    // 특정 과목의 선수과목 ID 목록 조회 (중복 제거)
    @Query("SELECT DISTINCT cp.prerequisiteId FROM CoursePrerequisite cp WHERE cp.course.id = :courseId AND cp.deletedAt IS NULL")
    List<Long> findPrerequisiteIdsByCourseId(@Param("courseId") Long courseId);
    
    // 특정 과목이 선수과목으로 사용되는 과목 목록 조회
    @Query("SELECT cp.course FROM CoursePrerequisite cp WHERE cp.prerequisiteId = :prerequisiteId AND cp.deletedAt IS NULL")
    List<Course> findCoursesByPrerequisiteId(@Param("prerequisiteId") Long prerequisiteId);
    
    // 특정 과목의 선수과목 관계 조회
    List<CoursePrerequisite> findByCourseId(Long courseId);
    
    // 특정 과목이 선수과목으로 사용되는 관계 조회
    List<CoursePrerequisite> findByPrerequisiteId(Long prerequisiteId);
    
    // 특정 과목과 선수과목 간의 관계 존재 여부 확인
    @Query("SELECT COUNT(cp) > 0 FROM CoursePrerequisite cp WHERE cp.course.id = :courseId AND cp.prerequisiteId = :prerequisiteId AND cp.deletedAt IS NULL")
    boolean existsByCourseIdAndPrerequisiteId(@Param("courseId") Long courseId, @Param("prerequisiteId") Long prerequisiteId);
    
    // 특정 과목의 모든 선수과목 관계 삭제 (소프트 삭제)
    @Modifying
    @Query("UPDATE CoursePrerequisite cp SET cp.deletedAt = CURRENT_TIMESTAMP WHERE cp.course.id = :courseId AND cp.deletedAt IS NULL")
    void deleteByCourseId(@Param("courseId") Long courseId);
    
    // 모든 선수과목 관계 삭제 (소프트 삭제)
    @Modifying
    @Query("UPDATE CoursePrerequisite cp SET cp.deletedAt = CURRENT_TIMESTAMP WHERE cp.deletedAt IS NULL")
    void deleteAllSoft();
}

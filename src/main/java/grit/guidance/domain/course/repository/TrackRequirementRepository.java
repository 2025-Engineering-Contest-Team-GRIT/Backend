package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.TrackRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRequirementRepository extends JpaRepository<TrackRequirement, Long> {
    // 특정 트랙의 졸업 요건(필수 과목 리스트) 가져오기
    List<TrackRequirement> findByTrackId(Long trackId);
    
    // 특정 과목의 트랙 요구사항 가져오기
    List<TrackRequirement> findByCourseId(Long courseId);
    
    // 여러 트랙의 전공필수/전공기초 과목 조회
    @Query("SELECT tr FROM TrackRequirement tr WHERE tr.track.id IN :trackIds AND tr.courseType IN ('MANDATORY', 'FOUNDATION') AND tr.deletedAt IS NULL")
    List<TrackRequirement> findByTrackIdsAndCourseType(@Param("trackIds") List<Long> trackIds);
    
    @Modifying
    @Query("UPDATE TrackRequirement tr SET tr.deletedAt = CURRENT_TIMESTAMP WHERE tr.deletedAt IS NULL")
    void deleteAllSoft();
}
package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.TrackRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRequirementRepository extends JpaRepository<TrackRequirement, Long> {
    // 특정 트랙의 졸업 요건(필수 과목 리스트) 가져오기
    List<TrackRequirement> findByTrackId(Long trackId);
    
    @Modifying
    @Query("UPDATE TrackRequirement tr SET tr.deletedAt = CURRENT_TIMESTAMP WHERE tr.deletedAt IS NULL")
    void deleteAllSoft();
}
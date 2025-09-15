package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.TrackRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRequirementRepository extends JpaRepository<TrackRequirement, Long> {
    
    @Modifying
    @Query("UPDATE TrackRequirement tr SET tr.deletedAt = CURRENT_TIMESTAMP WHERE tr.deletedAt IS NULL")
    void deleteAllSoft();
}
package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.TrackRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRequirementRepository extends JpaRepository<TrackRequirement, Long> {
}
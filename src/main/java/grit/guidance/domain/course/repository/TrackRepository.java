package grit.guidance.domain.course.repository;

import grit.guidance.domain.course.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    Optional<Track> findByTrackName(String trackName);
}
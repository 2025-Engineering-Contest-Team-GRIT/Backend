package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.UserTrack;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTrackRepository extends JpaRepository<UserTrack, Long> {
    void deleteByUser(Users user);
}
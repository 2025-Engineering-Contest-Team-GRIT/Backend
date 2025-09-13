package grit.guidance.domain.user.repository;

import grit.guidance.domain.course.entity.Track;
import grit.guidance.domain.user.entity.UserTrack;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTrackRepository extends JpaRepository<UserTrack, Long> {
    @Modifying
    @Query("DELETE FROM UserTrack ut WHERE ut.users = :user")
    void deleteByUsers(@Param("user") Users user);
    
    // 중복 체크: 사용자와 트랙이 모두 같은 사용자 트랙이 있는지 확인
    boolean existsByUsersAndTrack(Users user, Track track);
}
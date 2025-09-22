package grit.guidance.domain.user.repository;

import grit.guidance.domain.user.entity.FavoriteCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteCourseRepository extends JpaRepository<FavoriteCourse, Long> {
    
    // 사용자로 관심과목 목록 조회
    List<FavoriteCourse> findByUsers(Users user);
    
    // 사용자와 과목으로 관심과목 조회 (중복 체크용)
    @Query("SELECT COUNT(fc) > 0 FROM FavoriteCourse fc WHERE fc.users = :user AND fc.course.id = :courseId")
    boolean existsByUsersAndCourseId(@Param("user") Users user, @Param("courseId") Long courseId);
    
    // 사용자 ID로 관심과목 ID 목록 조회
    @Query("SELECT fc.course.id FROM FavoriteCourse fc WHERE fc.users.id = :userId")
    List<Long> findCourseIdsByUserId(@Param("userId") Long userId);
    
    // 사용자와 과목으로 관심과목 삭제
    @Modifying
    @Query("DELETE FROM FavoriteCourse fc WHERE fc.users = :user AND fc.course.id = :courseId")
    void deleteByUsersAndCourseId(@Param("user") Users user, @Param("courseId") Long courseId);
    
    // 사용자의 모든 관심과목 삭제
    @Modifying
    @Query("DELETE FROM FavoriteCourse fc WHERE fc.users = :user")
    void deleteByUsers(@Param("user") Users user);
}

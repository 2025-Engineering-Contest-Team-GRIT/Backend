package grit.guidance.domain.user.repository;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompletedCourseRepository extends JpaRepository<CompletedCourse, Long> {

    @Modifying
    @Query("DELETE FROM CompletedCourse cc WHERE cc.users = :user")
    void deleteByUsers(@Param("user") Users user);

    // [수정] 이 부분을 아래와 같이 바꿔주세요.
    // 기존의 findByUsers는 Course 정보를 지연 로딩(LAZY)하기 때문에 문제가 발생합니다.
    // JOIN FETCH를 사용하여 CompletedCourse와 관련된 Course 정보를 한 번의 쿼리로 즉시 로딩(EAGER)하도록 변경합니다.
    @Query("SELECT cc FROM CompletedCourse cc JOIN FETCH cc.course WHERE cc.users = :user")
    List<CompletedCourse> findByUsers(@Param("user") Users user);

    // 중복 체크: 사용자, 과목, 연도, 학기가 모두 같은 완료 과목이 있는지 확인
    boolean existsByUsersAndCourseAndCompletedYearAndCompletedSemester(
            Users user, Course course, int completedYear, Semester completedSemester);

    // 사용자 ID로 이수한 과목 ID 목록 조회
    @Query("SELECT cc.course.id FROM CompletedCourse cc WHERE cc.users.id = :userId")
    List<Long> findCourseIdsByUserId(@Param("userId") Long userId);
}
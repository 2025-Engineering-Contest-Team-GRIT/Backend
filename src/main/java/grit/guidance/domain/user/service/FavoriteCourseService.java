package grit.guidance.domain.user.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.user.dto.FavoriteCourseRequest;
import grit.guidance.domain.user.dto.FavoriteCourseResponse;
import grit.guidance.domain.user.entity.FavoriteCourse;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.FavoriteCourseRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import grit.guidance.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteCourseService {

    private final FavoriteCourseRepository favoriteCourseRepository;
    private final UsersRepository usersRepository;
    private final CourseRepository courseRepository;
    private final JwtService jwtService;

    @Transactional
    public FavoriteCourseResponse addFavoriteCourse(String studentId, FavoriteCourseRequest request) {
        try {
            // 0. 요청 데이터 검증
            if (request.courseId() == null) {
                log.warn("courseId가 null입니다: request={}", request);
                return new FavoriteCourseResponse(400, "courseId는 필수입니다.");
            }

            // 1. 사용자 조회
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 2. 과목 조회
            Optional<Course> courseOpt = courseRepository.findById(request.courseId());
            if (courseOpt.isEmpty()) {
                log.warn("과목을 찾을 수 없음: courseId={}", request.courseId());
                return FavoriteCourseResponse.notFound();
            }
            Course course = courseOpt.get();

            // 3. 중복 체크
            if (favoriteCourseRepository.existsByUsersAndCourseId(user, request.courseId())) {
                log.warn("이미 관심과목으로 등록됨: userId={}, courseId={}", user.getId(), request.courseId());
                return FavoriteCourseResponse.alreadyExists();
            }

            // 4. 관심과목 추가
            FavoriteCourse favoriteCourse = FavoriteCourse.builder()
                    .users(user)
                    .course(course)
                    .build();
            
            favoriteCourseRepository.save(favoriteCourse);
            
            log.info("관심과목 추가 완료: userId={}, courseId={}, courseName={}", 
                    user.getId(), request.courseId(), course.getCourseName());

            return FavoriteCourseResponse.success();

        } catch (Exception e) {
            log.error("관심과목 추가 실패: studentId={}, courseId={}, error={}", 
                    studentId, request.courseId(), e.getMessage(), e);
            return FavoriteCourseResponse.serverError();
        }
    }

    @Transactional
    public FavoriteCourseResponse removeFavoriteCourse(String studentId, Long courseId) {
        try {
            // 1. 사용자 조회
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 2. 관심과목 삭제
            favoriteCourseRepository.deleteByUsersAndCourseId(user, courseId);
            
            log.info("관심과목 삭제 완료: userId={}, courseId={}", user.getId(), courseId);

            return new FavoriteCourseResponse(200, "관심과목이 성공적으로 삭제되었습니다.");

        } catch (Exception e) {
            log.error("관심과목 삭제 실패: studentId={}, courseId={}, error={}", 
                    studentId, courseId, e.getMessage(), e);
            return FavoriteCourseResponse.serverError();
        }
    }
}

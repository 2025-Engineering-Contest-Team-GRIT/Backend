package grit.guidance.domain.roadmap.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.roadmap.entity.RecommendedCourse;
import grit.guidance.domain.roadmap.repository.RecommendedCourseRepository;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendedCourseService {

    private static final Logger log = LoggerFactory.getLogger(RecommendedCourseService.class);
    
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final CourseRepository courseRepository;
    private final UsersRepository usersRepository;

    //로드맵 추천 결과를 저장
    @Transactional
    public void saveRecommendedCourses(String studentId, List<Long> trackIds, List<Map<String, Object>> roadMap) {
        try {
            log.info("로드맵 추천 결과 저장 시작 - studentId: {}, trackIds: {}", studentId, trackIds);

            // 1. 사용자 조회
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("학번 " + studentId + "에 해당하는 사용자를 찾을 수 없습니다."));

            // 2. 기존 추천 과목 삭제
            recommendedCourseRepository.deleteByUser(user);
            log.info("기존 추천 과목 삭제 완료 - studentId: {}", studentId);

            // 3. 새로운 추천 과목 저장
            int totalSaved = 0;
            for (Map<String, Object> semester : roadMap) {
                Integer recommendYear = (Integer) semester.get("recommendYear");
                String semesterStr = (String) semester.get("recommendSemester");

                @SuppressWarnings("unchecked") // 컴파일 경고 억제 (현재 해당 타입이 확실함.)
                List<Map<String, Object>> courses = (List<Map<String, Object>>) semester.get("courses");

                if (courses != null) {
                    for (Map<String, Object> courseData : courses) {
                        String courseCode = (String) courseData.get("courseCode");
                        String courseName = (String) courseData.get("courseName");
                        String recommendDescription = (String) courseData.get("recommendDescription");

                        // 과목 조회
                        Optional<Course> courseOpt = courseRepository.findByCourseCode(courseCode);
                        if (courseOpt.isPresent()) {
                            Course course = courseOpt.get();
                            
                            // Semester enum 변환
                            Semester semesterEnum = "FIRST".equals(semesterStr) ? Semester.FIRST : Semester.SECOND;

                            // RecommendedCourse 생성 및 저장
                            RecommendedCourse recommendedCourse = RecommendedCourse.builder()
                                    .user(user)
                                    .course(course)
                                    .recommendDescription(recommendDescription)
                                    .recommendGrade(recommendYear)
                                    .recommendSemester(semesterEnum)
                                    .build();

                            recommendedCourseRepository.save(recommendedCourse);
                            totalSaved++;
                            
                            log.debug("추천 과목 저장 완료 - {}: {} ({})", 
                                    courseCode, courseName, recommendYear + "학년 " + semesterStr);
                        } else {
                            log.warn("과목을 찾을 수 없음 - courseCode: {}", courseCode);
                        }
                    }
                }
            }

            log.info("로드맵 추천 결과 저장 완료 - 총 {}개 과목 저장", totalSaved);

        } catch (Exception e) {
            log.error("로드맵 추천 결과 저장 실패 - studentId: {}, trackIds: {}", studentId, trackIds, e);
            throw new RuntimeException("로드맵 추천 결과 저장에 실패했습니다.", e);
        }
    }

}

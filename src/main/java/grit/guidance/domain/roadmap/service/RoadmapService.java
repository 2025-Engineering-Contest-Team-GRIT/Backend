package grit.guidance.domain.roadmap.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.CourseType;
import grit.guidance.domain.course.entity.Track;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
import grit.guidance.domain.course.repository.CoursePrerequisiteRepository;
import grit.guidance.domain.roadmap.dto.*;
import grit.guidance.domain.roadmap.entity.RecommendedCourse;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.EnrolledCourse;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.entity.UserTrack;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.EnrolledCourseRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.domain.roadmap.repository.RecommendedCourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapService {

    private final UsersRepository usersRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final EnrolledCourseRepository enrolledCourseRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final TrackRequirementRepository trackRequirementRepository;
    private final UserTrackRepository userTrackRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;

    public RoadmapResponseDto getRoadmapData(String studentId) {
        try {
            // 1. 사용자 정보 조회
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 2. 이수 완료 과목 조회
            List<CompletedCourse> completedCourses = completedCourseRepository.findByUsers(user);
            log.info("이수 완료 과목 수: {}", completedCourses.size());

            // 3. 수강 중인 과목 조회
            List<EnrolledCourse> enrolledCourses = enrolledCourseRepository.findByUser(user);
            log.info("수강 중인 과목 수: {}", enrolledCourses.size());

            // 4. 추천 과목 조회
            List<RecommendedCourse> recommendedCourses = recommendedCourseRepository.findAllByUser(user);
            log.info("추천 과목 수: {}", recommendedCourses.size());

            // 5. 사용자 트랙 정보 조회
            List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
            
            // 6. 학기별로 그룹화하여 로드맵 데이터 생성
            List<SemesterDto> semesters = buildSemesterData(completedCourses, enrolledCourses, recommendedCourses, userTracks);

            // 7. 로드맵 데이터 생성 (학년, 학기 정보 포함)
            RoadmapDataDto data = new RoadmapDataDto(
                    semesters, 
                    user.getGrade(), 
                    user.getSemester().toString()
            );
            return RoadmapResponseDto.success(data);

        } catch (Exception e) {
            log.error("로드맵 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return RoadmapResponseDto.serverError();
        }
    }

    private List<SemesterDto> buildSemesterData(List<CompletedCourse> completedCourses, 
                                               List<EnrolledCourse> enrolledCourses, 
                                               List<RecommendedCourse> recommendedCourses,
                                               List<UserTrack> userTracks) {
        
        // 모든 과목을 학기별로 그룹화
        Map<String, List<CourseDto>> semesterMap = new HashMap<>();

        // 1. 이수 완료 과목 처리
        for (CompletedCourse completedCourse : completedCourses) {
            Course course = completedCourse.getCourse();
            String semesterKey = completedCourse.getGradeLevel() + "_" + completedCourse.getCompletedSemester().ordinal();
            
            // 이수 완료 과목의 courseType 조회
            String courseType = getCourseTypeForCompletedCourse(course.getId(), completedCourse.getTrack());
            
            // 선수과목 ID 조회
            List<Long> prerequisiteIds = coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(course.getId());
            
            CourseDto courseDto = CourseDto.createCompleted(
                    course.getId(),
                    course.getCourseName(),
                    course.getCredits(),
                    courseType,
                    prerequisiteIds,
                    course.getDescription(),
                    completedCourse.getCompletedGrade().name()
            );

            semesterMap.computeIfAbsent(semesterKey, k -> new ArrayList<>()).add(courseDto);
        }

        // 2. 수강 중인 과목 처리
        for (EnrolledCourse enrolledCourse : enrolledCourses) {
            Course course = enrolledCourse.getCourse();
            // 현재 학기로 가정 (실제로는 사용자의 현재 학기 정보 필요)
            String semesterKey = getCurrentSemesterKey();
            
            // 수강 중인 과목의 courseType 조회
            String courseType = getCourseTypeForEnrolledOrRecommendedCourse(course.getId(), userTracks);
            
            // 선수과목 ID 조회
            List<Long> prerequisiteIds = coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(course.getId());
            
            CourseDto courseDto = CourseDto.createTaking(
                    course.getId(),
                    course.getCourseName(),
                    course.getCredits(),
                    courseType,
                    prerequisiteIds,
                    course.getDescription()
            );

            semesterMap.computeIfAbsent(semesterKey, k -> new ArrayList<>()).add(courseDto);
        }

        // 3. 추천 과목 처리
        for (RecommendedCourse recommendedCourse : recommendedCourses) {
            Course course = recommendedCourse.getCourse();
            String semesterKey = recommendedCourse.getRecommendGrade() + "_" + recommendedCourse.getRecommendSemester().ordinal();
            
            // 추천 과목의 courseType 조회
            String courseType = getCourseTypeForEnrolledOrRecommendedCourse(course.getId(), userTracks);
            
            // 선수과목 ID 조회
            List<Long> prerequisiteIds = coursePrerequisiteRepository.findPrerequisiteIdsByCourseId(course.getId());
            
            CourseDto courseDto = CourseDto.createRecommended(
                    course.getId(),
                    course.getCourseName(),
                    course.getCredits(),
                    courseType,
                    prerequisiteIds,
                    course.getDescription(),
                    recommendedCourse.getRecommendDescription()
            );

            semesterMap.computeIfAbsent(semesterKey, k -> new ArrayList<>()).add(courseDto);
        }

        // 4. 학기별 데이터를 SemesterDto로 변환
        List<SemesterDto> semesters = new ArrayList<>();
        for (Map.Entry<String, List<CourseDto>> entry : semesterMap.entrySet()) {
            String[] parts = entry.getKey().split("_");
            int year = Integer.parseInt(parts[0]); // 이제 gradeLevel이 들어감
            int semester = Integer.parseInt(parts[1]) + 1; // 0-based to 1-based
            
            int totalCredits = entry.getValue().stream()
                    .mapToInt(CourseDto::credits)
                    .sum();

            semesters.add(new SemesterDto(year, semester, totalCredits, entry.getValue()));
        }

        // 학기 순으로 정렬
        semesters.sort((a, b) -> {
            int yearCompare = a.year().compareTo(b.year());
            return yearCompare != 0 ? yearCompare : a.semester().compareTo(b.semester());
        });

        return semesters;
    }

    private String getCurrentSemesterKey() {
        // 현재 학기 계산 로직 (임시로 3학년 1학기로 설정)
        return "3_0"; // 3학년 1학기
    }

    /**
     * 이수 완료 과목의 courseType 조회
     */
    private String getCourseTypeForCompletedCourse(Long courseId, Track track) {
        if (track == null) {
            return "일반선택";
        }
        
        Optional<CourseType> courseType = trackRequirementRepository
                .findCourseTypeByCourseIdAndTrackId(courseId, track.getId());
        
        return courseType.map(CourseType::getDescription).orElse("일반선택");
    }

    /**
     * 수강 중인 과목이나 추천 과목의 courseType 조회
     * PRIMARY 트랙 먼저 검색, 없으면 SECONDARY 트랙 검색, 그래도 없으면 "일반선택"
     */
    private String getCourseTypeForEnrolledOrRecommendedCourse(Long courseId, List<UserTrack> userTracks) {
        // PRIMARY 트랙 찾기
        Optional<UserTrack> primaryTrack = userTracks.stream()
                .filter(ut -> ut.getTrackType().name().equals("PRIMARY"))
                .findFirst();
        
        if (primaryTrack.isPresent()) {
            Optional<CourseType> courseType = trackRequirementRepository
                    .findCourseTypeByCourseIdAndTrackId(courseId, primaryTrack.get().getTrack().getId());
            if (courseType.isPresent()) {
                return courseType.get().getDescription();
            }
        }
        
        // SECONDARY 트랙 찾기
        Optional<UserTrack> secondaryTrack = userTracks.stream()
                .filter(ut -> ut.getTrackType().name().equals("SECONDARY"))
                .findFirst();
        
        if (secondaryTrack.isPresent()) {
            Optional<CourseType> courseType = trackRequirementRepository
                    .findCourseTypeByCourseIdAndTrackId(courseId, secondaryTrack.get().getTrack().getId());
            if (courseType.isPresent()) {
                return courseType.get().getDescription();
            }
        }
        
        return "일반선택";
    }
}

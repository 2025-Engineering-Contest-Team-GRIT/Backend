package grit.guidance.domain.user.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.CourseType;
import grit.guidance.domain.course.entity.TrackRequirement;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
import grit.guidance.domain.user.dto.UserCourseDto;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.EnrolledCourse;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.EnrolledCourseRepository;
import grit.guidance.domain.user.repository.FavoriteCourseRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import grit.guidance.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCourseService {

    private final TrackRequirementRepository trackRequirementRepository;
    private final UsersRepository usersRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final EnrolledCourseRepository enrolledCourseRepository;
    private final FavoriteCourseRepository favoriteCourseRepository;
    private final JwtService jwtService;

    /**
     * 사용자의 1, 2 트랙에 있는 과목들을 상태별로 조회
     * @param studentId 사용자 학번
     * @return 사용자별 과목 목록 (상태 정보 포함)
     */
    public List<UserCourseDto> getUserCoursesByTrack(String studentId) {
        log.info("사용자 과목 조회 시작 - studentId: {}", studentId);

        // 1. 사용자 정보 조회 (학년, 학기 정보를 위해)
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + studentId));

        // 2. 사용자의 1, 2 트랙 과목들 조회 (TrackRequirement 기반)
        List<TrackRequirement> trackRequirements = trackRequirementRepository.findByTrackIds(List.of(1L, 2L));
        List<Course> trackCourses = trackRequirements.stream()
                .map(TrackRequirement::getCourse)
                .distinct()
                .collect(Collectors.toList());
        log.info("1, 2 트랙 과목 수: {}", trackCourses.size());

        // 3. 사용자의 완료된 과목들 조회
        List<CompletedCourse> completedCourses = completedCourseRepository.findByUsersStudentId(studentId);
        Map<Long, CompletedCourse> completedCourseMap = completedCourses.stream()
                .collect(Collectors.toMap(cc -> cc.getCourse().getId(), cc -> cc));

        // 4. 사용자의 수강 중인 과목들 조회
        List<EnrolledCourse> enrolledCourses = enrolledCourseRepository.findByUsersStudentId(studentId);
        Map<Long, EnrolledCourse> enrolledCourseMap = enrolledCourses.stream()
                .collect(Collectors.toMap(ec -> ec.getCourse().getId(), ec -> ec));

        // 5. 사용자의 관심과목들 조회
        List<Long> favoriteCourseIds = favoriteCourseRepository.findCourseIdsByUserId(user.getId());
        log.info("관심과목 수: {}", favoriteCourseIds.size());

        // 6. 과목별 트랙 요구사항 매핑
        Map<Long, List<TrackRequirement>> courseTrackRequirementsMap = trackRequirements.stream()
                .collect(Collectors.groupingBy(tr -> tr.getCourse().getId()));

        // 7. 과목별 상태 정보 생성
        List<UserCourseDto> userCourses = new ArrayList<>();
        
        for (Course course : trackCourses) {
            List<TrackRequirement> courseRequirements = courseTrackRequirementsMap.get(course.getId());
            
            // 과목의 트랙별 요구사항 분석
            String courseType = determineCourseType(courseRequirements);
            Long trackId = determineTrackId(courseRequirements, course.getId(), completedCourseMap);
            
            // 관심과목 여부 확인
            boolean isFavorite = favoriteCourseIds.contains(course.getId());
            
            UserCourseDto.UserCourseDtoBuilder builder = UserCourseDto.builder()
                    .course(course)
                    .courseType(courseType)
                    .trackId(trackId)
                    .isFavorite(isFavorite);

            if (completedCourseMap.containsKey(course.getId())) {
                // 완료된 과목 - completed_course 테이블에서 정보 가져오기
                CompletedCourse completedCourse = completedCourseMap.get(course.getId());
                builder.status("completed")
                       .grade(completedCourse.getGradeLevel())
                       .semester(completedCourse.getCompletedSemester().toString());
                log.debug("완료된 과목: {} (trackId: {})", course.getCourseName(), completedCourse.getTrack().getId());
            } else if (enrolledCourseMap.containsKey(course.getId())) {
                // 수강 중인 과목 - user 테이블에서 정보 가져오기
                builder.status("enrolled")
                       .grade(user.getGrade())
                       .semester(user.getSemester().toString());
                log.debug("수강 중인 과목: {}", course.getCourseName());
            } else {
                // 수강 가능한 과목 - course 테이블에서 정보 가져오기
                builder.status("available")
                       .grade(course.getOpenGrade())
                       .semester(course.getOpenSemester().toString());
                log.debug("수강 가능한 과목: {}", course.getCourseName());
            }

            userCourses.add(builder.build());
        }

        log.info("사용자 과목 조회 완료 - 총 {}개 과목 (완료: {}, 수강중: {}, 수강가능: {}, 관심과목: {})", 
                userCourses.size(),
                userCourses.stream().filter(uc -> "completed".equals(uc.getStatus())).count(),
                userCourses.stream().filter(uc -> "enrolled".equals(uc.getStatus())).count(),
                userCourses.stream().filter(uc -> "available".equals(uc.getStatus())).count(),
                userCourses.stream().filter(uc -> Boolean.TRUE.equals(uc.getIsFavorite())).count());

        return userCourses;
    }

    /**
     * 과목의 트랙별 요구사항을 분석하여 과목 타입 결정
     * @param courseRequirements 과목의 트랙별 요구사항 리스트
     * @return 과목 타입 ("전공필수", "전공선택", "전공기초", "일반선택")
     */
    private String determineCourseType(List<TrackRequirement> courseRequirements) {
        if (courseRequirements == null || courseRequirements.isEmpty()) {
            return "일반선택";
        }

        // 전공필수가 있으면 전공필수
        boolean hasMandatory = courseRequirements.stream()
                .anyMatch(tr -> tr.getCourseType() == CourseType.MANDATORY);
        if (hasMandatory) {
            return "전공필수";
        }

        // 전공기초가 있으면 전공기초
        boolean hasFoundation = courseRequirements.stream()
                .anyMatch(tr -> tr.getCourseType() == CourseType.FOUNDATION);
        if (hasFoundation) {
            return "전공기초";
        }

        // 둘 다 전공선택이면 전공선택
        boolean allElective = courseRequirements.stream()
                .allMatch(tr -> tr.getCourseType() == CourseType.ELECTIVE);
        if (allElective) {
            return "전공선택";
        }

        return "일반선택";
    }

    /**
     * 과목의 트랙 ID 결정
     * @param courseRequirements 과목의 트랙별 요구사항 리스트
     * @param courseId 과목 ID
     * @param completedCourseMap 완료된 과목 맵
     * @return 트랙 ID (전공필수일 때만, completed_course에 있을 때는 해당 트랙)
     */
    private Long determineTrackId(List<TrackRequirement> courseRequirements, Long courseId, Map<Long, CompletedCourse> completedCourseMap) {
        // 완료된 과목이면 해당 트랙 ID 반환
        if (completedCourseMap.containsKey(courseId)) {
            return completedCourseMap.get(courseId).getTrack().getId();
        }

        // 전공필수인 경우에만 트랙 ID 반환
        if (courseRequirements != null && !courseRequirements.isEmpty()) {
            boolean hasMandatory = courseRequirements.stream()
                    .anyMatch(tr -> tr.getCourseType() == CourseType.MANDATORY);
            if (hasMandatory) {
                // 전공필수인 트랙 중 첫 번째 반환
                return courseRequirements.stream()
                        .filter(tr -> tr.getCourseType() == CourseType.MANDATORY)
                        .map(tr -> tr.getTrack().getId())
                        .findFirst()
                        .orElse(null);
            }
        }

        return null;
    }

    /**
     * JWT 토큰에서 사용자 학번을 추출하여 과목 조회
     * @param authorization Authorization 헤더
     * @return 사용자별 과목 목록
     */
    public List<UserCourseDto> getUserCoursesByToken(String authorization) {
        log.info("JWT 토큰으로 사용자 과목 조회 시작");
        
        try {
            // JWT 토큰에서 학번 추출
            String token = authorization.replace("Bearer ", "");
            String studentId = jwtService.getStudentIdFromToken(token);
            
            if (studentId == null || studentId.isEmpty()) {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }
            
            log.info("토큰에서 추출된 학번: {}", studentId);
            return getUserCoursesByTrack(studentId);
            
        } catch (Exception e) {
            log.error("JWT 토큰 처리 실패", e);
            throw new RuntimeException("토큰 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
package grit.guidance.domain.simulation.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.TrackRequirement;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import grit.guidance.domain.graduation.service.GraduationService;
import grit.guidance.domain.simulation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.simulation.dto.SimulationDto;
import grit.guidance.domain.simulation.dto.SimulationDto.CourseDetailDto;
import grit.guidance.domain.simulation.dto.SimulationDto.PlanDetailDto;
import grit.guidance.domain.simulation.entity.GraduationPlan;
import grit.guidance.domain.simulation.entity.GraduationPlanCourse;
import grit.guidance.domain.simulation.repository.GraduationPlanCourseRepository;
import grit.guidance.domain.simulation.repository.GraduationPlanRepository;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.UserTrack;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import grit.guidance.domain.user.dto.UserTrackDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimulationService {

    private final UsersRepository usersRepository;
    private final UserTrackRepository userTrackRepository;
    private final TrackRequirementRepository trackRequirementRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final CourseRepository courseRepository;
    private final GraduationPlanRepository graduationPlanRepository;
    private final GraduationPlanCourseRepository graduationPlanCourseRepository;
    private final GraduationService graduationService;

    /**
     * 시뮬레이션 초기 데이터 조회 메서드
     */
    public SimulationDto.SimulationDataResponseDto getSimulationData(String studentId) {
        GraduationResponseDto dashboardData = graduationService.getDashboardData(studentId);
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        // [추가] 사용자의 트랙 정보를 조회하고, 응답에 포함될 DTO 리스트로 변환합니다.
        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
        List<UserTrackDto> userTrackDtos = userTracks.stream()
                .map(ut -> UserTrackDto.builder()
                        .trackId(ut.getTrack().getId())
                        .trackName(ut.getTrack().getTrackName())
                        .build())
                .collect(Collectors.toList());

        // [수정] 헬퍼 메소드를 호출할 때, 위에서 조회한 userTracks 정보를 넘겨주어 DB 조회를 한번 줄입니다.
        List<SimulationDto.AvailableCourseDto> availableCourses = getAvailableCoursesForUser(user, userTracks);

        // [수정] 최종 응답 DTO를 생성할 때, userTracks 정보를 함께 담아줍니다.
        return SimulationDto.SimulationDataResponseDto.builder()
                .crawlingData(dashboardData)
                .availableCourses(availableCourses)
                .userTracks(userTrackDtos)
                .build();
    }

    // ==========================================================
    // == 졸업 계획 생성, 조회, 수정, 삭제(CRUD) 로직 ==
    // ==========================================================

    /**
     * CREATE: 새로운 졸업 계획 생성
     */
        @Transactional
    public Long createGraduationPlan(String studentId, GraduationPlanRequestDto requestDto) {
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        GraduationPlan plan = GraduationPlan.builder()
                .planName(requestDto.getPlanName())
                .users(user)
                .build();
        GraduationPlan savedPlan = graduationPlanRepository.save(plan);

        List<GraduationPlanRequestDto.SelectedCourse> selectedCourses = requestDto.getSelectedCourses();
        if (selectedCourses != null && !selectedCourses.isEmpty()) {
            List<String> courseCodes = selectedCourses.stream()
                    .map(GraduationPlanRequestDto.SelectedCourse::getCourseCode)
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findAllByCourseCodeIn(courseCodes);
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getCourseCode, c -> c));

            for (GraduationPlanRequestDto.SelectedCourse selectedCourse : selectedCourses) {
                Course course = courseMap.get(selectedCourse.getCourseCode());
                if (course != null) {
                    GraduationPlanCourse gpc = GraduationPlanCourse.builder()
                            .graduationPlan(savedPlan)
                            .course(course)
                            .track(selectedCourse.getTrackId())
                            .build();
                    graduationPlanCourseRepository.save(gpc);
                }
            }
        }
        return savedPlan.getId();
    }

    /**
     * READ (List): 특정 사용자의 모든 졸업 계획 목록 조회
     */
    public List<SimulationDto.PlanSummaryDto> getGraduationPlans(String studentId) {
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        return graduationPlanRepository.findByUsers(user).stream()
                .map(plan -> SimulationDto.PlanSummaryDto.builder()
                        .planId(plan.getId())
                        .planName(plan.getPlanName())
                        .createdAt(plan.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * READ (Detail): 특정 졸업 계획의 상세 정보 조회
     */
    public PlanDetailDto getGraduationPlanDetail(Long planId) {
        GraduationPlan plan = graduationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("해당 졸업 계획을 찾을 수 없습니다. ID: " + planId));

        List<GraduationPlanCourse> planCourses = graduationPlanCourseRepository.findByGraduationPlanId(planId);

        List<CourseDetailDto> courseDetailDtos = planCourses.stream()
                .map(gpc -> CourseDetailDto.builder()
                        .courseCode(gpc.getCourse().getCourseCode())
                        .courseName(gpc.getCourse().getCourseName())
                        .credits(gpc.getCourse().getCredits())
                        .openGrade(gpc.getCourse().getOpenGrade())
                        .openSemester(gpc.getCourse().getOpenSemester())
                        .trackId(gpc.getTrack())
                        .build())
                .collect(Collectors.toList());

        return PlanDetailDto.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .createdAt(plan.getCreatedAt())
                .selectedCourses(courseDetailDtos)
                .build();
    }

    /**
     * UPDATE: 특정 졸업 계획 수정
     */
    @Transactional
    public void updateGraduationPlan(Long planId, GraduationPlanRequestDto requestDto) {
        GraduationPlan plan = graduationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("해당 졸업 계획을 찾을 수 없습니다. ID: " + planId));

        // 1. 계획 이름 업데이트
        plan.updatePlanName(requestDto.getPlanName());

        // 2. 기존에 연결된 과목 정보 삭제
        List<GraduationPlanCourse> existingCourses = graduationPlanCourseRepository.findByGraduationPlanId(planId);
        graduationPlanCourseRepository.deleteAll(existingCourses);

        // 3. 새로운 과목 정보 추가
        List<GraduationPlanRequestDto.SelectedCourse> selectedCourses = requestDto.getSelectedCourses();
        if (selectedCourses != null && !selectedCourses.isEmpty()) {
            List<String> courseCodes = selectedCourses.stream()
                    .map(GraduationPlanRequestDto.SelectedCourse::getCourseCode)
                    .collect(Collectors.toList());

            List<Course> courses = courseRepository.findAllByCourseCodeIn(courseCodes);
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getCourseCode, c -> c));

            for (GraduationPlanRequestDto.SelectedCourse selectedCourse : selectedCourses) {
                Course course = courseMap.get(selectedCourse.getCourseCode());
                if (course != null) {
                    GraduationPlanCourse gpc = GraduationPlanCourse.builder()
                            .graduationPlan(plan)
                            .course(course)
                            .track(selectedCourse.getTrackId())
                            .build();
                    graduationPlanCourseRepository.save(gpc);
                }
            }
        }
    }

    /**
     * DELETE: 특정 졸업 계획 삭제
     */
    @Transactional
    public void deleteGraduationPlan(Long planId) {
        GraduationPlan plan = graduationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 계획을 찾을 수 없습니다. ID: " + planId));

        graduationPlanRepository.delete(plan);
    }


    /**
     * 수강 가능 과목 목록 조회 헬퍼 메소드 (핵심 로직 수정 부분)
     */
    private List<SimulationDto.AvailableCourseDto> getAvailableCoursesForUser(Users user, List<UserTrack> userTracks) {
        Set<String> completedCourseCodes = completedCourseRepository.findByUsers(user).stream()
                .map(completedCourse -> completedCourse.getCourse().getCourseCode())
                .collect(Collectors.toSet());

        List<TrackRequirement> trackRequirements = new ArrayList<>();
        for (UserTrack userTrack : userTracks) {
            trackRequirements.addAll(trackRequirementRepository.findByTrackId(userTrack.getTrack().getId()));
        }

        // [수정] 과목(Course)을 기준으로 모든 관련 정보를 List로 그룹핑하여, 더 많은 정보를 한 번에 처리할 수 있도록 구조를 변경했습니다.
        Map<Course, List<TrackRequirement>> courseToRequirementsMap = trackRequirements.stream()
                .filter(req -> !"FOUNDATION".equals(req.getCourseType().name()))
                .collect(Collectors.groupingBy(TrackRequirement::getCourse));

        return courseToRequirementsMap.entrySet().stream()
                .filter(entry -> !completedCourseCodes.contains(entry.getKey().getCourseCode()))
                .map(entry -> {
                    Course course = entry.getKey();
                    List<TrackRequirement> requirements = entry.getValue();

                    boolean isMandatory = requirements.stream()
                            .anyMatch(req -> "MANDATORY".equals(req.getCourseType().name()));
                    String finalCourseType = isMandatory ? "MANDATORY" : "ELECTIVE";

                    // [추가] 그룹핑된 정보를 바탕으로, 해당 과목이 속한 모든 트랙의 ID 목록을 생성합니다.
                    List<Long> applicableTrackIds = requirements.stream()
                            .map(req -> req.getTrack().getId())
                            .distinct()
                            .collect(Collectors.toList());

                    // [수정] 최종 DTO를 생성할 때, 요청하신 모든 추가 정보를 채워 넣습니다.
                    return SimulationDto.AvailableCourseDto.builder()
                            .courseCode(course.getCourseCode())
                            .courseName(course.getCourseName())
                            .credit(course.getCredits())
                            .courseType(finalCourseType)
                            .openGrade(course.getOpenGrade())             // <-- 추가된 정보
                            .openSemester(course.getOpenSemester())     // <-- 추가된 정보
                            .applicableTrackIds(applicableTrackIds)     // <-- 추가된 정보
                            .build();
                })
                .collect(Collectors.toList());
    }


}


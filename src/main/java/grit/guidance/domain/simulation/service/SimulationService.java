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

        Set<String> completedCourseCodes = completedCourseRepository.findByUsers(user).stream()
                .map(completedCourse -> completedCourse.getCourse().getCourseCode())
                .collect(Collectors.toSet());

        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
        List<Long> trackIds = userTracks.stream()
                .map(userTrack -> userTrack.getTrack().getId())
                .toList();

        List<TrackRequirement> trackRequirements = new ArrayList<>();
        for (Long trackId : trackIds) {
            trackRequirements.addAll(trackRequirementRepository.findByTrackId(trackId));
        }

        Map<Course, String> processedCourses = new HashMap<>();
        for (TrackRequirement req : trackRequirements) {
            Course course = req.getCourse();
            String currentType = req.getCourseType().name();
            if ("FOUNDATION".equals(currentType)) continue;

            if (processedCourses.containsKey(course)) {
                if ("ELECTIVE".equals(processedCourses.get(course)) && "MANDATORY".equals(currentType)) {
                    processedCourses.put(course, currentType);
                }
            } else {
                processedCourses.put(course, currentType);
            }
        }

        List<SimulationDto.AvailableCourseDto> availableCourses = processedCourses.entrySet().stream()
                .filter(entry -> !completedCourseCodes.contains(entry.getKey().getCourseCode()))
                .map(entry -> SimulationDto.AvailableCourseDto.builder()
                        .courseCode(entry.getKey().getCourseCode())
                        .courseName(entry.getKey().getCourseName())
                        .credit(entry.getKey().getCredits())
                        .courseType(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return SimulationDto.SimulationDataResponseDto.builder()
                .crawlingData(dashboardData)
                .availableCourses(availableCourses)
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

        List<String> courseCodes = requestDto.getSelectedCourseCodes();
        if (courseCodes != null && !courseCodes.isEmpty()) {
            List<GraduationPlanCourse> planCourses = courseCodes.stream()
                    .map(code -> {
                        Course course = courseRepository.findByCourseCode(code)
                                .orElseThrow(() -> new IllegalArgumentException("과목을 찾을 수 없습니다. 코드: " + code));
                        return GraduationPlanCourse.builder()
                                .graduationPlan(savedPlan)
                                .course(course)
                                .build();
                    })
                    .toList();
            graduationPlanCourseRepository.saveAll(planCourses);
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
                .orElseThrow(() -> new IllegalArgumentException("조회할 계획을 찾을 수 없습니다. ID: " + planId));

        List<CourseDetailDto> courseDetails = plan.getGraduationPlanCourses().stream()
                .map(planCourse -> {
                    Course course = planCourse.getCourse();
                    return CourseDetailDto.builder()
                            .courseCode(course.getCourseCode())
                            .courseName(course.getCourseName())
                            .credits(course.getCredits())
                            .build();
                })
                .collect(Collectors.toList());

        return PlanDetailDto.builder()
                .planId(plan.getId())
                .planName(plan.getPlanName())
                .createdAt(plan.getCreatedAt())
                .selectedCourses(courseDetails)
                .build();
    }

    /**
     * UPDATE: 특정 졸업 계획 수정
     */
    @Transactional
    public void updateGraduationPlan(Long planId, GraduationPlanRequestDto requestDto) {
        GraduationPlan plan = graduationPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 계획을 찾을 수 없습니다. ID: " + planId));

        plan.updatePlanName(requestDto.getPlanName());
        plan.getGraduationPlanCourses().clear();

        List<String> courseCodes = requestDto.getSelectedCourseCodes();
        if (courseCodes != null && !courseCodes.isEmpty()) {
            courseCodes.forEach(code -> {
                Course course = courseRepository.findByCourseCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("과목을 찾을 수 없습니다. 코드: " + code));
                GraduationPlanCourse newPlanCourse = GraduationPlanCourse.builder()
                        .graduationPlan(plan)
                        .course(course)
                        .build();
                plan.getGraduationPlanCourses().add(newPlanCourse);
            });
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
}


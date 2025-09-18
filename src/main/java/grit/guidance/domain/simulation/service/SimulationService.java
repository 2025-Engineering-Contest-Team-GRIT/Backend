package grit.guidance.domain.simulation.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.TrackRequirement;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import grit.guidance.domain.graduation.service.GraduationService;
import grit.guidance.domain.simulation.dto.SimulationDto;
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
    private final GraduationService graduationService;

    public SimulationDto.SimulationDataResponseDto getSimulationData(String studentId) {
        GraduationResponseDto dashboardData = graduationService.getDashboardData(studentId);

        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        // [수정 1] CompletedCourse에서 Course 객체를 거쳐 courseCode를 가져오도록 람다식을 수정합니다.
        Set<String> completedCourseCodes = completedCourseRepository.findByUsers(user).stream()
                .map(completedCourse -> completedCourse.getCourse().getCourseCode())
                .collect(Collectors.toSet());

        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
        List<Long> trackIds = userTracks.stream()
                .map(userTrack -> userTrack.getTrack().getId())
                .toList();

        // [수정 2] findByTrackIdIn 대신, 반복문을 사용하여 각 트랙의 요구사항을 조회하고 하나의 리스트로 합칩니다.
        List<TrackRequirement> trackRequirements = new ArrayList<>();
        for (Long trackId : trackIds) {
            trackRequirements.addAll(trackRequirementRepository.findByTrackId(trackId));
        }

        Map<Course, String> processedCourses = new HashMap<>();

        for (TrackRequirement req : trackRequirements) {
            Course course = req.getCourse();
            String currentType = req.getCourseType().name();

            if ("FOUNDATION".equals(currentType)) {
                continue;
            }

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
}


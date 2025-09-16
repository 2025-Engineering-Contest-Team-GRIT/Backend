package grit.guidance.domain.graduation.service;

import grit.guidance.domain.user.entity.TrackType;
import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.graduation.dto.CertificationStatusDto;
import grit.guidance.domain.graduation.dto.DashboardResponseDto;
import grit.guidance.domain.graduation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.graduation.dto.TrackProgressDto;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.GraduationRequirement;
import grit.guidance.domain.user.entity.UserTrack;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.GraduationRequirementRepository;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationService {

    private final UsersRepository usersRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;
    private final UserTrackRepository userTrackRepository;

    public DashboardResponseDto getDashboardData(String studentId) { // ⭐ HansungDataResponse 인자 제거
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        GraduationRequirement requirement = graduationRequirementRepository.findByUsers(user)
                .orElse(GraduationRequirement.builder()
                        .users(user)
                        .capstoneCompleted(false)
                        .thesisSubmitted(false)
                        .awardOrCertificateReceived(false)
                        .build());

        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
        List<String> orderedTrackNames = userTracks.stream()
                .sorted((a, b) -> a.getTrackType().name().compareTo(b.getTrackType().name()))
                .map(ut -> ut.getTrack().getTrackName())
                .collect(Collectors.toList());

        List<CompletedCourse> completedCourses = completedCourseRepository.findByUsers(user);
        Map<String, Integer> completedCreditsByTrack = new LinkedHashMap<>();
        Set<Long> countedCourseIds = new HashSet<>();

        for (CompletedCourse cc : completedCourses) {
            Course course = cc.getCourse();
            if (countedCourseIds.contains(course.getId())) {
                continue;
            }
            if (cc.getTrack() != null) {
                String trackName = cc.getTrack().getTrackName();
                if (orderedTrackNames.contains(trackName)) {
                    completedCreditsByTrack.merge(trackName, course.getCredits(), Integer::sum);
                    countedCourseIds.add(course.getId());
                }
            }
        }

        final int REQUIRED_PER_TRACK = 39;
        List<TrackProgressDto> trackProgressList = orderedTrackNames.stream()
                .map(trackName -> {
                    int completed = completedCreditsByTrack.getOrDefault(trackName, 0);
                    int required = REQUIRED_PER_TRACK;
                    double progress = required > 0 ? (completed * 100.0) / required : 0;
                    return TrackProgressDto.builder()
                            .trackName(trackName)
                            .category("트랙")
                            .completedCredits(completed)
                            .requiredCredits(required)
                            .remainingCredits(Math.max(0, required - completed))
                            .progressRate(progress)
                            .build();
                })
                .collect(Collectors.toList());

        int totalCompletedCredits = completedCourses.stream()
                .mapToInt(cc -> cc.getCourse().getCredits())
                .sum();

        int totalRequiredCredits = 130;

        List<CertificationStatusDto> certifications = List.of(
                CertificationStatusDto.builder().certificationName("캡스톤디자인 발표회 작품 출품").isCompleted(requirement.getCapstoneCompleted()).build(),
                CertificationStatusDto.builder().certificationName("졸업 논문").isCompleted(requirement.getThesisSubmitted()).build(),
                CertificationStatusDto.builder().certificationName("전공 관련 자격증/공모전 입상").isCompleted(requirement.getAwardOrCertificateReceived()).build()
        );

        return DashboardResponseDto.builder()
                .totalCompletedCredits(totalCompletedCredits)
                .totalRequiredCredits(totalRequiredCredits)
                .trackProgressList(trackProgressList)
                .certifications(certifications)
                .build();
    }

    @Transactional
    public void saveGraduationPlan(GraduationPlanRequestDto planDto) {
        // ...
    }
}
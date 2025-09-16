package grit.guidance.domain.graduation.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.CourseType;
import grit.guidance.domain.course.entity.TrackRequirement;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
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
    private final TrackRequirementRepository trackRequirementRepository;
    private final UserTrackRepository userTrackRepository;

    /**
     * 대시보드 데이터 조회
     */
    public DashboardResponseDto getDashboardData(String studentId) {
        // 1. 사용자 조회
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        // 2. 이수 과목 & 졸업 요건 조회
        List<CompletedCourse> completedCourses = completedCourseRepository.findByUsers(user);
        GraduationRequirement requirement = graduationRequirementRepository.findByUsers(user)
                .orElse(GraduationRequirement.builder()
                        .users(user)
                        .capstoneCompleted(false)
                        .thesisSubmitted(false)
                        .awardOrCertificateReceived(false)
                        .build());

        // 3. 사용자 선택 트랙 조회 (PRIMARY/SECONDARY)
        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
        // PRIMARY 우선, SECONDARY 다음 순서로 정렬
        List<String> orderedTrackNames = userTracks.stream()
                .sorted((a, b) -> a.getTrackType().name().compareTo(b.getTrackType().name()))
                .map(ut -> ut.getTrack().getTrackName())
                .collect(Collectors.toList());
        Set<String> selectedTrackNames = new LinkedHashSet<>(orderedTrackNames);

        // 선택된 트랙이 없다면 전체 트랙 기준이 아닌 빈 결과로 처리
        if (selectedTrackNames.isEmpty()) {
            return DashboardResponseDto.builder()
                    .totalCompletedCredits(0)
                    .totalRequiredCredits(0)
                    .trackProgressList(Collections.emptyList())
                    .certifications(List.of(
                            CertificationStatusDto.builder().certificationName("캡스톤디자인 발표회 작품 출품").isCompleted(requirement.getCapstoneCompleted()).build(),
                            CertificationStatusDto.builder().certificationName("졸업 논문").isCompleted(requirement.getThesisSubmitted()).build(),
                            CertificationStatusDto.builder().certificationName("전공 관련 자격증/공모전 입상").isCompleted(requirement.getAwardOrCertificateReceived()).build()
                    ))
                    .build();
        }

        // 4. 트랙 요구 사항 조회 (사용자 선택 트랙만 - 추론 용도)
        List<TrackRequirement> trackRequirements = trackRequirementRepository.findAll().stream()
                .filter(tr -> selectedTrackNames.contains(tr.getTrack().getTrackName()))
                .collect(Collectors.toList());

        // 4-1. 트랙별 요구 학점: 정책상 각 트랙 39학점 고정
        final int REQUIRED_PER_TRACK = 39;
        Map<String, Integer> requiredCreditsByTrack = selectedTrackNames.stream()
                .collect(Collectors.toMap(name -> name, name -> REQUIRED_PER_TRACK, (a, b) -> a, LinkedHashMap::new));

        // 5. 학생 이수 과목 → 트랙별 학점 매핑 (사용자 선택 트랙만)
        Map<String, Integer> completedCreditsByTrack = new LinkedHashMap<>();
        // 중복 집계를 방지하기 위한 과목 단위 집계 세트
        Set<Long> countedCourseIds = new HashSet<>();
        for (CompletedCourse cc : completedCourses) {
            Course course = cc.getCourse();
            Long courseId = course.getId();
            if (countedCourseIds.contains(courseId)) {
                continue;
            }
            // 우선 이수 과목에 명시된 트랙이 있으면 해당 트랙에만 합산
            if (cc.getTrack() != null) {
                String trackName = cc.getTrack().getTrackName();
                if (selectedTrackNames.contains(trackName)) {
                    completedCreditsByTrack.merge(trackName, course.getCredits(), Integer::sum);
                    countedCourseIds.add(courseId);
                }
                continue;
            }

            // 트랙 정보가 비어 있으면, 과목이 어떤 선택 트랙의 요건에 포함되는지로 추론하여 합산(최초로 매칭되는 트랙)
            for (String candidateTrackName : orderedTrackNames) {
                boolean belongsToCandidate = trackRequirements.stream()
                        .anyMatch(tr -> Objects.equals(tr.getCourse().getId(), courseId)
                                && tr.getTrack().getTrackName().equals(candidateTrackName));
                if (belongsToCandidate) {
                    completedCreditsByTrack.merge(candidateTrackName, course.getCredits(), Integer::sum);
                    countedCourseIds.add(courseId);
                    break;
                }
            }

        }

        // 6. 트랙별 진행 상황 DTO 생성 (트랙당 최대 39학점 캡 적용)
        List<TrackProgressDto> trackProgressList = requiredCreditsByTrack.entrySet().stream()
                .map(entry -> {
                    String trackName = entry.getKey();
                    int required = entry.getValue();
                    int completedRaw = completedCreditsByTrack.getOrDefault(trackName, 0);
                    int completedCapped = Math.min(required, completedRaw);
                    int remaining = Math.max(0, required - completedCapped);
                    double progress = required == 0 ? 0 : (completedCapped * 100.0) / required;
                    return TrackProgressDto.builder()
                            .trackName(trackName)
                            .category("트랙")
                            .completedCredits(completedCapped)
                            .requiredCredits(required)
                            .remainingCredits(remaining)
                            .progressRate(progress)
                            .build();
                })
                .collect(Collectors.toList());

        // 7. 총 학점 계산 (선택 트랙 기준 합계)
        int totalCompletedCredits = trackProgressList.stream().mapToInt(tp -> tp.getCompletedCredits()).sum();
        int totalRequiredCredits = selectedTrackNames.size() * REQUIRED_PER_TRACK;

        // 8. 졸업 인증 상태
        List<CertificationStatusDto> certifications = List.of(
                CertificationStatusDto.builder()
                        .certificationName("캡스톤디자인 발표회 작품 출품")
                        .isCompleted(requirement.getCapstoneCompleted())
                        .build(),
                CertificationStatusDto.builder()
                        .certificationName("졸업 논문")
                        .isCompleted(requirement.getThesisSubmitted())
                        .build(),
                CertificationStatusDto.builder()
                        .certificationName("전공 관련 자격증/공모전 입상")
                        .isCompleted(requirement.getAwardOrCertificateReceived())
                        .build()
        );

        // 9. 결과 반환
        return DashboardResponseDto.builder()
                .totalCompletedCredits(totalCompletedCredits)
                .totalRequiredCredits(totalRequiredCredits)
                .trackProgressList(trackProgressList)
                .certifications(certifications)
                .build();
    }

    /**
     * 졸업 시뮬레이션 저장
     */
    @Transactional
    public void saveGraduationPlan(GraduationPlanRequestDto planDto) {
        // TODO: GraduationPlan & GraduationPlanCourse 저장 로직 작성
    }
}

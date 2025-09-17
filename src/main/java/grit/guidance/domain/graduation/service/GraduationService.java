package grit.guidance.domain.graduation.service;

import grit.guidance.domain.graduation.dto.CertificationStatusDto;
import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import grit.guidance.domain.graduation.dto.TrackProgressDto;
import grit.guidance.domain.graduation.entity.CrawlingGraduation;
import grit.guidance.domain.graduation.repository.CrawlingGraduationRepository;
import grit.guidance.domain.user.entity.GraduationRequirement;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.GraduationRequirementRepository;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationService {

    private final UsersRepository usersRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;
    private final CrawlingGraduationRepository crawlingGraduationRepository;
    private final UserTrackRepository userTrackRepository;

    public GraduationResponseDto getDashboardData(String studentId) {
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        // 1. 크롤링된 학점 정보 조회
        CrawlingGraduation crawlingData = crawlingGraduationRepository.findByUsers(user)
                .orElseThrow(() -> new IllegalStateException("크롤링된 졸업 데이터를 찾을 수 없습니다. 먼저 크롤링을 수행해주세요."));

        // 2. 졸업 인증 요건 조회
        GraduationRequirement requirement = graduationRequirementRepository.findByUsers(user)
                .orElse(GraduationRequirement.builder()
                        .users(user)
                        .capstoneCompleted(false)
                        .thesisSubmitted(false)
                        .awardOrCertificateReceived(false)
                        .build());

        // 3. 트랙별 진행률 계산 및 DTO 생성 (크롤링 데이터 활용)
        // 사용자의 1, 2트랙 이름 가져오기
        List<String> orderedTrackNames = userTrackRepository.findByUsers(user).stream()
                .sorted((a, b) -> a.getTrackType().name().compareTo(b.getTrackType().name()))
                .map(ut -> ut.getTrack().getTrackName())
                .toList();

        final int REQUIRED_PER_TRACK = 39;

        // 크롤링 데이터를 기반으로 TrackProgressDto 생성
        TrackProgressDto track1Progress = TrackProgressDto.builder()
                .trackName(orderedTrackNames.get(0))
                .category("트랙")
                .completedCredits(crawlingData.getTrack1MajorSubtotal())
                .requiredCredits(REQUIRED_PER_TRACK)
                .remainingCredits(Math.max(0, REQUIRED_PER_TRACK - crawlingData.getTrack1MajorSubtotal()))
                .progressRate((double) crawlingData.getTrack1MajorSubtotal() * 100 / REQUIRED_PER_TRACK)
                .build();

        TrackProgressDto track2Progress = TrackProgressDto.builder()
                .trackName(orderedTrackNames.get(1))
                .category("트랙")
                .completedCredits(crawlingData.getTrack2MajorSubtotal())
                .requiredCredits(REQUIRED_PER_TRACK)
                .remainingCredits(Math.max(0, REQUIRED_PER_TRACK - crawlingData.getTrack2MajorSubtotal()))
                .progressRate((double) crawlingData.getTrack2MajorSubtotal() * 100 / REQUIRED_PER_TRACK)
                .build();

        List<TrackProgressDto> trackProgressList = List.of(track1Progress, track2Progress);

        // 4. 졸업 인증 상태 DTO 생성
        List<CertificationStatusDto> certifications = List.of(
                CertificationStatusDto.builder().certificationName("캡스톤디자인 발표회 작품 출품").isCompleted(requirement.getCapstoneCompleted()).build(),
                CertificationStatusDto.builder().certificationName("졸업 논문").isCompleted(requirement.getThesisSubmitted()).build(),
                CertificationStatusDto.builder().certificationName("전공 관련 자격증/공모전 입상").isCompleted(requirement.getAwardOrCertificateReceived()).build()
        );

        // 5. 최종 응답 DTO 생성 및 반환
        return GraduationResponseDto.from(crawlingData, trackProgressList, certifications);
    }
}
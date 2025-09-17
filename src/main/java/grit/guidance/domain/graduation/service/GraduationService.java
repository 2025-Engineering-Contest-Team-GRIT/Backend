package grit.guidance.domain.graduation.service;

import grit.guidance.domain.graduation.dto.CertificationStatusDto;
import grit.guidance.domain.graduation.dto.DetailedCreditDto;
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

        // 3. 트랙별 DTO 생성 (크롤링 데이터 활용)
        List<String> orderedTrackNames = userTrackRepository.findByUsers(user).stream()
                .sorted((a, b) -> a.getTrackType().name().compareTo(b.getTrackType().name()))
                .map(ut -> ut.getTrack().getTrackName())
                .toList();

        // 트랙1 상세 학점
        DetailedCreditDto track1MajorBasic = DetailedCreditDto.builder()
                .completedCredits(crawlingData.getTrack1MajorBasic())
                .requiredCredits(3)
                .build();

        DetailedCreditDto track1MajorRequired = DetailedCreditDto.builder()
                .completedCredits(crawlingData.getTrack1MajorRequired())
                .requiredCredits(15)
                .build();

        DetailedCreditDto track1MajorSubtotal = DetailedCreditDto.builder()
                .completedCredits(crawlingData.getTrack1MajorSubtotal())
                .requiredCredits(39)
                .build();

        // 트랙1 진행 상황
        TrackProgressDto track1Progress = TrackProgressDto.builder()
                .trackName(orderedTrackNames.get(0))
                .category("트랙")
                .majorBasic(track1MajorBasic)
                .majorRequired(track1MajorRequired)
                .majorSubtotal(track1MajorSubtotal)
                .build();

        // 트랙2 상세 학점
        DetailedCreditDto track2MajorBasic = DetailedCreditDto.builder()
                .completedCredits(crawlingData.getTrack2MajorBasic())
                .requiredCredits(3)
                .build();

        DetailedCreditDto track2MajorRequired = DetailedCreditDto.builder()
                .completedCredits(crawlingData.getTrack2MajorRequired())
                .requiredCredits(15)
                .build();

        DetailedCreditDto track2MajorSubtotal = DetailedCreditDto.builder()
                .completedCredits(crawlingData.getTrack2MajorSubtotal())
                .requiredCredits(39)
                .build();

        // 트랙2 진행 상황
        TrackProgressDto track2Progress = TrackProgressDto.builder()
                .trackName(orderedTrackNames.get(1))
                .category("트랙")
                .majorBasic(track2MajorBasic)
                .majorRequired(track2MajorRequired)
                .majorSubtotal(track2MajorSubtotal)
                .build();

        List<TrackProgressDto> trackProgressList = List.of(track1Progress, track2Progress);

        // 4. 졸업 인증 상태 DTO 생성
        List<CertificationStatusDto> certifications = List.of(
                CertificationStatusDto.builder().certificationName("캡스톤디자인 발표회 작품 출품").isCompleted(requirement.getCapstoneCompleted()).build(),
                CertificationStatusDto.builder().certificationName("졸업 논문").isCompleted(requirement.getThesisSubmitted()).build(),
                CertificationStatusDto.builder().certificationName("전공 관련 자격증/공모전 입상").isCompleted(requirement.getAwardOrCertificateReceived()).build()
        );

        // 5. 최종 응답 DTO 생성 및 반환
        return GraduationResponseDto.builder()
                .totalCompletedCredits(crawlingData.getTotalCompletedCredits())
                .totalRequiredCredits(crawlingData.getTotalMajorRequired())
                .trackProgressList(trackProgressList)
                .certifications(certifications)
                .build();
    }



        // ⭐ 졸업 요건 상태 업데이트 로직 구현
        @Transactional
        public void updateCertificationStatus(String studentId, String type, boolean isCompleted) {
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

            GraduationRequirement requirement = graduationRequirementRepository.findByUsers(user)
                    .orElseThrow(() -> new IllegalStateException("졸업 요건 정보를 찾을 수 없습니다."));

            switch (type) {
                case "capstone":
                    // ⭐ 메서드 이름 수정
                    requirement.updateCapstoneStatus(isCompleted);
                    break;
                case "thesis":
                    // ⭐ 메서드 이름 수정
                    requirement.updateThesisStatus(isCompleted);
                    break;
                case "award":
                    // ⭐ 메서드 이름 수정
                    requirement.updateAwardStatus(isCompleted);
                    break;
                default:
                    throw new IllegalArgumentException("유효하지 않은 졸업 요건 타입입니다: " + type);
            }

            graduationRequirementRepository.save(requirement); // 변경된 상태 저장
            log.info("사용자 {}의 졸업 요건 '{}' 상태가 '{}'로 업데이트되었습니다.", studentId, type, isCompleted);
        }
    }

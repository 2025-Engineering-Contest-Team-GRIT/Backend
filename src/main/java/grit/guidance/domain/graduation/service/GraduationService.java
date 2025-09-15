package grit.guidance.domain.graduation.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.CourseType;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.graduation.dto.CertificationStatusDto;
import grit.guidance.domain.graduation.dto.DashboardResponseDto;
import grit.guidance.domain.graduation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.graduation.dto.TrackProgressDto;
import grit.guidance.domain.user.entity.CompletedCourse;
import grit.guidance.domain.user.entity.GraduationRequirement;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.GraduationRequirementRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationService {

    private final UsersRepository usersRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;
    private final CourseRepository courseRepository;
    // 필요한 다른 리포지토리가 있다면 여기에 추가합니다.

    // 대시보드에 필요한 데이터를 조회하는 메서드입니다.
    public DashboardResponseDto getDashboardData(String studentId) {
        // 1. 사용자 정보 가져오기
        Users user = usersRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. 학번: " + studentId));

        // 2. 이수 과목과 졸업 요건 데이터 가져오기
        List<CompletedCourse> completedCourses = completedCourseRepository.findByUsers(user);
        GraduationRequirement requirement = graduationRequirementRepository.findByUsers(user)
                .orElseThrow(() -> new IllegalArgumentException("졸업 요건 정보를 찾을 수 없습니다."));

        // 3. 총 학점과 트랙별 학점을 한 번에 계산
        int totalCompletedCredits = 0;
        int majorBasicsCredits = 0;
        int majorRequiredCredits = 0;
        int majorElectiveCredits = 0;
        int liberalArtsCredits = 0;

        for (CompletedCourse cc : completedCourses) {
            if (cc.getCourse() != null) {
                // 총 학점 합산
                totalCompletedCredits += cc.getCourse().getCredits();

                // CourseType Enum으로 변환 후 switch문에서 사용
                try {
                    CourseType courseType = CourseType.valueOf(cc.getCourse().getType());
                    switch (courseType) {
                        case FOUNDATION:
                            majorBasicsCredits += cc.getCourse().getCredits();
                            break;
                        case MANDATORY:
                            majorRequiredCredits += cc.getCourse().getCredits();
                            break;
                        case ELECTIVE:
                            majorElectiveCredits += cc.getCourse().getCredits();
                            break;
                        case GENERAL_ELECTIVE:
                            liberalArtsCredits += cc.getCourse().getCredits();
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    // Enum 변환 실패 시 로그 기록
                    log.error("알 수 없는 과목 유형입니다: {}", cc.getCourse().getType(), e);
                }
            }
        }

        // 4. 트랙별 학점 DTO 생성 (요구 학점은 임시값)
        List<TrackProgressDto> trackProgressList = new ArrayList<>();
        trackProgressList.add(TrackProgressDto.builder()
                .trackName("전공기초")
                .category("전공기초")   // category도 추가
                .completedCredits(majorBasicsCredits)
                .requiredCredits(3)
                .build());

        trackProgressList.add(TrackProgressDto.builder()
                .trackName("전공필수")
                .category("전공필수")
                .completedCredits(majorRequiredCredits)
                .requiredCredits(15)
                .build());

        trackProgressList.add(TrackProgressDto.builder()
                .trackName("전공소계")
                .category("전공소계")
                .completedCredits(majorBasicsCredits + majorRequiredCredits + majorElectiveCredits)
                .requiredCredits(39)
                .build());

        // 5. 졸업 인증 상태 DTO 생성
        List<CertificationStatusDto> certifications = new ArrayList<>();
        certifications.add(CertificationStatusDto.builder()
                .certificationName("캡스톤디자인 발표회 작품 출품")
                .isCompleted(requirement.getCapstoneCompleted())
                .build());
        certifications.add(CertificationStatusDto.builder()
                .certificationName("졸업 논문")
                .isCompleted(requirement.getThesisSubmitted()) // 메서드 이름 수정
                .build());
        certifications.add(CertificationStatusDto.builder()
                .certificationName("전공 관련 자격증/공모전 입상")
                .isCompleted(requirement.getAwardOrCertificateReceived()) // 메서드 이름 수정
                .build());

        // 6. 모든 정보를 DTO에 담아 반환
        return DashboardResponseDto.builder()
                .totalCompletedCredits(totalCompletedCredits)
                .totalRequiredCredits(130)
                .trackProgressList(trackProgressList)
                .certifications(certifications)
                .build();
    }

    // 시뮬레이션 결과를 저장하는
    // 메서드입니다.
    @Transactional
    public void saveGraduationPlan(GraduationPlanRequestDto planDto) {
        // 실제 구현 로직은 다음과 같습니다.
        // 1. usersRepository를 사용하여 planDto의 studentId에 해당하는 사용자 엔티티를 찾습니다.
        // 2. GraduationPlanRequestDto의 정보로 GraduationPlan 엔티티를 생성합니다.
        // 3. GraduationPlanRepository를 사용하여 이 엔티티를 데이터베이스에 저장합니다.
        // 4. planDto의 selectedCourseCodes 목록을 순회하며, 각 과목에 대한
        //    GraduationPlanCourse 엔티티를 생성하고 저장합니다.
        // TODO: 위 로직을 여기에 직접 구현하세요.
    }
}
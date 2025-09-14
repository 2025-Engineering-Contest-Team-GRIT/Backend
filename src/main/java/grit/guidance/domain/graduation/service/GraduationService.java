package grit.guidance.domain.graduation.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.graduation.dto.CertificationStatusDto;
import grit.guidance.domain.graduation.dto.DashboardResponseDto;
import grit.guidance.domain.graduation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.graduation.dto.TrackProgressDto;
import grit.guidance.domain.user.entity.CompletedCourse; // Correct import for the entity
import grit.guidance.domain.user.entity.GraduationRequirement; // Correct import for the entity
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.CompletedCourseRepository; // Correct import for the repository
import grit.guidance.domain.user.repository.GraduationRequirementRepository; // Correct import for the repository
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        /*
        // 3. 총 이수 학점 계산
        int totalCompletedCredits = completedCourses.stream()
                .mapToInt(CompletedCourse::getCreditsEarned)
                .sum();

        // 4. 트랙별 학점 계산
        // 이 로직은 CompletedCourse가 Course 엔티티를 가지고 있다는 가정하에 작성되었습니다.
        int majorBasicsCredits = 0;
        int majorRequiredCredits = 0;
        int majorElectiveCredits = 0;
        int liberalArtsCredits = 0;

        for (CompletedCourse cc : completedCourses) {
            if (cc.getCourse() != null) {
                switch (cc.getCourse().getType()) {
                    case "전공기초":
                        majorBasicsCredits += cc.getCreditsEarned();
                        break;
                    case "전공필수":
                        majorRequiredCredits += cc.getCreditsEarned();
                        break;
                    case "전공선택":
                        majorElectiveCredits += cc.getCreditsEarned();
                        break;
                    case "교양":
                        liberalArtsCredits += cc.getCreditsEarned();
                        break;
                }
            }
        }

        // TODO: ERD의 track_requirement 테이블을 사용하여 실제 필요 학점과 비교하는 로직을 추가해야 합니다.
        // 현재는 예시 값으로 고정합니다.
        List<TrackProgressDto> trackProgressList = new ArrayList<>();
        trackProgressList.add(TrackProgressDto.builder()
                .trackName("전공기초")
                .completedCredits(majorBasicsCredits)
                .requiredCredits(3)
                .build());
        trackProgressList.add(TrackProgressDto.builder()
                .trackName("전공필수")
                .completedCredits(majorRequiredCredits)
                .requiredCredits(15)
                .build());
        trackProgressList.add(TrackProgressDto.builder()
                .trackName("전공소계")
                .completedCredits(majorBasicsCredits + majorRequiredCredits + majorElectiveCredits)
                .requiredCredits(39)
                .build());

        // 5. 졸업 인증 상태 계산
        List<CertificationStatusDto> certifications = new ArrayList<>();
        certifications.add(CertificationStatusDto.builder()
                .certificationName("캡스톤디자인 발표회 작품 출품")
                .isCompleted(requirement.isCapstoneCompleted())
                .build());
        certifications.add(CertificationStatusDto.builder()
                .certificationName("졸업 논문")
                .isCompleted(requirement.isFixedSubmitted())
                .build());
        certifications.add(CertificationStatusDto.builder()
                .certificationName("전공 관련 자격증/공모전 입상")
                .isCompleted(requirement.isIsCertificateReceived())
                .build());

        // 6. DTO에 모든 정보 담아 반환
        return DashboardResponseDto.builder()
                .totalCompletedCredits(totalCompletedCredits)
                .totalRequiredCredits(130) // TODO: 실제 총 필요 학점 로직으로 변경
                .trackProgressList(trackProgressList)
                .certifications(certifications)
                .build();

            */
        return null;
    }

    // 시뮬레이션 결과를 저장하는 메서드입니다.
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
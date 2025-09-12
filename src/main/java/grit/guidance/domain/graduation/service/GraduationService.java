package grit.guidance.domain.graduation.service;

import grit.guidance.domain.graduation.dto.DashboardResponseDto;
import grit.guidance.domain.graduation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.graduation.entity.GraduationPlan;
import grit.guidance.domain.graduation.repository.GraduationPlanRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GraduationService {

    private final UsersRepository usersRepository;
    private final GraduationPlanRepository graduationPlanRepository;

    // 대시보드에 필요한 데이터를 조회하는 메서드입니다.
    public DashboardResponseDto getDashboardData(String studentId) {
        // 실제 구현 로직은 다음과 같습니다.
        // 1. usersRepository를 사용하여 studentId에 해당하는 사용자 정보를 가져옵니다.
        // 2. completedCourseRepository를 사용하여 사용자가 이수한 과목 목록을 가져옵니다.
        // 3. graduationRequirementRepository를 사용하여 사용자의 졸업 요건 정보를 가져옵니다.
        // 4. 이 데이터를 바탕으로 총 학점, 트랙별 학점, 졸업 인증 상태 등을 계산합니다.
        // 5. 계산된 정보를 DashboardResponseDto에 담아 반환합니다.

        // TODO: 위 로직을 여기에 직접 구현하세요. 아래는 예시 코드입니다.
        return DashboardResponseDto.builder()
                .totalCompletedCredits(37)
                .totalRequiredCredits(130)
                .trackProgressList(null) // TODO: 실제 데이터로 채우기
                .certifications(null)    // TODO: 실제 데이터로 채우기
                .build();
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
        System.out.println("계획 저장 로직 실행: " + planDto.getPlanName());
    }
}
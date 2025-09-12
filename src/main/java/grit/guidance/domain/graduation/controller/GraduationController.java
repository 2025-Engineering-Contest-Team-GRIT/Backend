package grit.guidance.domain.graduation.controller;

import grit.guidance.domain.graduation.dto.DashboardResponseDto;
import grit.guidance.domain.graduation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.graduation.service.GraduationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graduation") // 이 컨트롤러의 기본 주소는 /api/graduation 입니다.
@RequiredArgsConstructor
public class GraduationController {

    private final GraduationService graduationService;

    // 1. 대시보드 조회 API
    // GET /api/graduation/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDto> getDashboardData(@RequestParam String studentId) {
        // 서비스 계층의 메서드를 호출하여 대시보드에 필요한 데이터를 가져옵니다.
        DashboardResponseDto dashboardData = graduationService.getDashboardData(studentId);
        return ResponseEntity.ok(dashboardData);
    }

    // 2. 시뮬레이션 결과 저장 API
    // POST /api/graduation/simulation
    @PostMapping("/simulation")
    public ResponseEntity<Void> saveGraduationPlan(@RequestBody GraduationPlanRequestDto planDto) {
        // 프론트엔드에서 받은 시뮬레이션 결과를 서비스로 전달하여 저장합니다.
        graduationService.saveGraduationPlan(planDto);
        return ResponseEntity.ok().build();
    }
}
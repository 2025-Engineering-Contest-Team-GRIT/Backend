package grit.guidance.domain.graduation.controller;

import grit.guidance.domain.graduation.dto.DashboardResponseDto;
import grit.guidance.domain.graduation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.graduation.service.GraduationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graduation")
@RequiredArgsConstructor
public class GraduationController {

    private final GraduationService graduationService;

    // 1. 대시보드 조회 API
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDto> getDashboardData(@RequestParam String studentId) {
        try {
            // ⭐ 크롤링 로직을 제거하고, DB에서 데이터를 가져오도록 서비스만 호출
            DashboardResponseDto dashboardData = graduationService.getDashboardData(studentId);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 2. 시뮬레이션 결과 저장 API
    @PostMapping("/simulation")
    public ResponseEntity<Void> saveGraduationPlan(@RequestBody GraduationPlanRequestDto planDto) {
        graduationService.saveGraduationPlan(planDto);
        return ResponseEntity.ok().build();
    }
}
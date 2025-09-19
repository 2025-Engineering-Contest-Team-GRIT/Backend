package grit.guidance.domain.simulation.controller;

import grit.guidance.domain.simulation.dto.GraduationPlanRequestDto;
import grit.guidance.domain.simulation.dto.SimulationDto;
import grit.guidance.domain.simulation.dto.SimulationDto.PlanDetailDto;
import grit.guidance.domain.simulation.service.SimulationService;
import grit.guidance.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * API 1: 시뮬레이션 초기 데이터 조회
     * (현재 이수 현황 + 수강 가능 과목 목록)
     */
    @GetMapping("/data")
    public ApiResponse<SimulationDto.SimulationDataResponseDto> getSimulationData(@RequestParam String studentId) {
        SimulationDto.SimulationDataResponseDto data = simulationService.getSimulationData(studentId);
        return ApiResponse.onSuccess(data);
    }

    // ==========================================================
    // == 졸업 계획 CRUD API 엔드포인트 ==
    // ==========================================================

    /**
     * API 2: 졸업 계획 생성 (Create)
     */
    @PostMapping("/plan")
    public ApiResponse<Long> createPlan(@RequestParam String studentId, @RequestBody GraduationPlanRequestDto requestDto) {
        Long planId = simulationService.createGraduationPlan(studentId, requestDto);
        return ApiResponse.onSuccess("새로운 졸업 계획이 저장되었습니다.", planId);
    }

    /**
     * API 3: 졸업 계획 목록 조회 (Read)
     */
    @GetMapping("/plans")
    public ApiResponse<List<SimulationDto.PlanSummaryDto>> getPlans(@RequestParam String studentId) {
        List<SimulationDto.PlanSummaryDto> plans = simulationService.getGraduationPlans(studentId);
        return ApiResponse.onSuccess(plans);
    }

    /**
     * API 3-1: 특정 졸업 계획 상세 조회 (Read Detail)
     */
    @GetMapping("/plan/{planId}")
    public ApiResponse<PlanDetailDto> getPlanDetail(@PathVariable Long planId) {
        PlanDetailDto planDetail = simulationService.getGraduationPlanDetail(planId);
        return ApiResponse.onSuccess(planDetail);
    }

    /**
     * API 4: 졸업 계획 수정 (Update)
     */
    @PutMapping("/plan/{planId}")
    public ApiResponse<Void> updatePlan(@PathVariable Long planId, @RequestBody GraduationPlanRequestDto requestDto) {
        simulationService.updateGraduationPlan(planId, requestDto);
        return ApiResponse.onSuccess("졸업 계획이 성공적으로 수정되었습니다.", null);
    }

    /**
     * API 5: 졸업 계획 삭제 (Delete)
     */
    @DeleteMapping("/plan/{planId}")
    public ApiResponse<Void> deletePlan(@PathVariable Long planId) {
        simulationService.deleteGraduationPlan(planId);
        return ApiResponse.onSuccess("졸업 계획이 성공적으로 삭제되었습니다.", null);
    }
}


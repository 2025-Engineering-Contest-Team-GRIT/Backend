package grit.guidance.domain.simulation.controller;

import grit.guidance.domain.simulation.dto.SimulationDto;
import grit.guidance.domain.simulation.service.SimulationService;
import grit.guidance.global.common.response.ApiResponse; // 패키지 경로 확인
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/data")
    public ApiResponse<SimulationDto.SimulationDataResponseDto> getSimulationData(@RequestParam String studentId) {
        SimulationDto.SimulationDataResponseDto data = simulationService.getSimulationData(studentId);
        // ApiResponse.ok(data) -> ApiResponse.onSuccess(data)로 수정
        return ApiResponse.onSuccess(data);
    }

    // TODO: 졸업 계획 CRUD 관련 엔드포인트는 여기에 추가할 예정입니다.
}


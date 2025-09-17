package grit.guidance.domain.graduation.controller;

import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import grit.guidance.domain.graduation.service.GraduationService;
import grit.guidance.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/graduation")
public class GraduationController {

    private final GraduationService graduationService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<GraduationResponseDto>> getGraduationDashboard(@RequestParam String studentId) {
        GraduationResponseDto dashboardData = graduationService.getDashboardData(studentId);
        return ResponseEntity.ok(ApiResponse.onSuccess(dashboardData));
    }
}
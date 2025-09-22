package grit.guidance.domain.roadmap.controller;

import grit.guidance.domain.roadmap.dto.RoadmapResponseDto;
import grit.guidance.domain.roadmap.service.RoadmapService;
import grit.guidance.global.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/roadmaps")
@RequiredArgsConstructor
@Tag(name = "로드맵", description = "로드맵 관련 API")
public class RoadmapController {

    private final RoadmapService roadmapService;
    private final JwtService jwtService;

    @GetMapping
    @Operation(summary = "로드맵 조회", description = "사용자의 전체 로드맵 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로드맵 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<RoadmapResponseDto> getRoadmap(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        
        try {
            log.info("로드맵 요청 - Authorization 헤더: {}", authorization);
            
            // JWT 토큰 검증
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님: {}", authorization);
                return ResponseEntity.status(401).body(RoadmapResponseDto.unauthorized());
            }

            String token = authorization.substring(7); // "Bearer " 제거
            log.info("추출된 토큰: {}", token);
            
            // 토큰 유효성 검증
            if (!jwtService.validateToken(token)) {
                log.warn("토큰 유효성 검증 실패");
                return ResponseEntity.status(401).body(RoadmapResponseDto.unauthorized());
            }
            
            String studentId = jwtService.getStudentIdFromToken(token);
            log.info("토큰에서 추출된 학번: {}", studentId);
            
            if (studentId == null || studentId.trim().isEmpty()) {
                log.warn("학번이 null이거나 빈 문자열");
                return ResponseEntity.status(401).body(RoadmapResponseDto.unauthorized());
            }

            // 로드맵 데이터 조회
            RoadmapResponseDto response = roadmapService.getRoadmapData(studentId);
            
            if (response.status() == 200) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.status()).body(response);
            }

        } catch (Exception e) {
            log.error("로드맵 API 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(RoadmapResponseDto.serverError());
        }
    }
}
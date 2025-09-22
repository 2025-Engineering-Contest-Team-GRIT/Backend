package grit.guidance.domain.user.controller;

import grit.guidance.domain.user.dto.TimetableResponse;
import grit.guidance.domain.user.service.TimetableService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "시간표", description = "시간표 관련 API")
public class TimetableController {

    private final TimetableService timetableService;
    private final JwtService jwtService;

    @GetMapping("/timetable")
    @Operation(summary = "시간표 조회", description = "사용자의 시간표 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "시간표 조회 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<TimetableResponse> getTimetable(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        
        try {
            log.info("시간표 요청 - Authorization 헤더: {}", authorization);
            
            // JWT 토큰 검증
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님: {}", authorization);
                return ResponseEntity.status(401).body(TimetableResponse.unauthorized());
            }

            String token = authorization.substring(7); // "Bearer " 제거
            log.info("추출된 토큰: {}", token);
            
            // 토큰 유효성 검증
            if (!jwtService.validateToken(token)) {
                log.warn("토큰 유효성 검증 실패");
                return ResponseEntity.status(401).body(TimetableResponse.unauthorized());
            }
            
            String studentId = jwtService.getStudentIdFromToken(token);
            log.info("토큰에서 추출된 학번: {}", studentId);
            
            if (studentId == null || studentId.trim().isEmpty()) {
                log.warn("학번이 null이거나 빈 문자열");
                return ResponseEntity.status(401).body(TimetableResponse.unauthorized());
            }

            // 시간표 조회
            TimetableResponse response = timetableService.getTimetable(studentId);
            
            if (response.status() == 200) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.status()).body(response);
            }

        } catch (Exception e) {
            log.error("시간표 API 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(TimetableResponse.serverError());
        }
    }
}

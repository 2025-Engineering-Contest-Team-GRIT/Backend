package grit.guidance.domain.user.controller;

import grit.guidance.domain.user.dto.FavoriteCourseRequest;
import grit.guidance.domain.user.dto.FavoriteCourseResponse;
import grit.guidance.domain.user.service.FavoriteCourseService;
import grit.guidance.global.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "관심과목", description = "관심과목 관련 API")
public class FavoriteCourseController {

    private final FavoriteCourseService favoriteCourseService;
    private final JwtService jwtService;

    @PostMapping("/favorites")
    @Operation(summary = "관심과목 추가", description = "과목을 관심과목으로 추가합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "관심과목 추가 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요합니다"),
        @ApiResponse(responseCode = "404", description = "과목을 찾을 수 없습니다"),
        @ApiResponse(responseCode = "409", description = "이미 관심과목으로 등록된 과목입니다"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<FavoriteCourseResponse> addFavoriteCourse(
            @RequestBody FavoriteCourseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("관심과목 추가 요청: courseId={}", request.courseId());
            
            // JWT 토큰 검증
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님: {}", authorization);
                return ResponseEntity.status(401).body(FavoriteCourseResponse.unauthorized());
            }

            String token = authorization.substring(7); // "Bearer " 제거
            log.info("추출된 토큰: {}", token);
            
            // 토큰 유효성 검증
            if (!jwtService.validateToken(token)) {
                log.warn("토큰 유효성 검증 실패");
                return ResponseEntity.status(401).body(FavoriteCourseResponse.unauthorized());
            }
            
            String studentId = jwtService.getStudentIdFromToken(token);
            log.info("토큰에서 추출된 학번: {}", studentId);
            
            if (studentId == null || studentId.trim().isEmpty()) {
                log.warn("학번이 null이거나 빈 문자열");
                return ResponseEntity.status(401).body(FavoriteCourseResponse.unauthorized());
            }

            // 관심과목 추가
            FavoriteCourseResponse response = favoriteCourseService.addFavoriteCourse(studentId, request);
            
            if (response.status() == 200) {
                return ResponseEntity.ok(response);
            } else if (response.status() == 409) {
                return ResponseEntity.status(409).body(response);
            } else if (response.status() == 404) {
                return ResponseEntity.status(404).body(response);
            } else {
                return ResponseEntity.status(response.status()).body(response);
            }

        } catch (Exception e) {
            log.error("관심과목 추가 API 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(FavoriteCourseResponse.serverError());
        }
    }

    @DeleteMapping("/favorites/{courseId}")
    @Operation(summary = "관심과목 삭제", description = "관심과목에서 제거합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "관심과목 삭제 성공"),
        @ApiResponse(responseCode = "401", description = "로그인이 필요합니다"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<FavoriteCourseResponse> removeFavoriteCourse(
            @PathVariable Long courseId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest httpRequest) {
        
        try {
            log.info("관심과목 삭제 요청: courseId={}", courseId);
            
            // JWT 토큰 검증
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님: {}", authorization);
                return ResponseEntity.status(401).body(FavoriteCourseResponse.unauthorized());
            }

            String token = authorization.substring(7); // "Bearer " 제거
            
            // 토큰 유효성 검증
            if (!jwtService.validateToken(token)) {
                log.warn("토큰 유효성 검증 실패");
                return ResponseEntity.status(401).body(FavoriteCourseResponse.unauthorized());
            }
            
            String studentId = jwtService.getStudentIdFromToken(token);
            
            if (studentId == null || studentId.trim().isEmpty()) {
                log.warn("학번이 null이거나 빈 문자열");
                return ResponseEntity.status(401).body(FavoriteCourseResponse.unauthorized());
            }

            // 관심과목 삭제
            FavoriteCourseResponse response = favoriteCourseService.removeFavoriteCourse(studentId, courseId);
            
            if (response.status() == 200) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.status()).body(response);
            }

        } catch (Exception e) {
            log.error("관심과목 삭제 API 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(FavoriteCourseResponse.serverError());
        }
    }
}

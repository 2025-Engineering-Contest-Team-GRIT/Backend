package grit.guidance.domain.user.controller;

import grit.guidance.domain.user.dto.UserCourseDto;
import grit.guidance.domain.user.service.UserCourseService;
import grit.guidance.global.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserCourseService userCourseService;
    private final JwtService jwtService;

    @GetMapping("/courses")
    @Operation(summary = "사용자 과목 조회", 
              description = "사용자의 1, 2 트랙에 있는 과목들을 상태별로 조회합니다. " +
                           "완료된 과목은 trackId를 포함하고, 모든 과목의 학년과 학기 정보를 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "과목 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<List<UserCourseDto>> getUserCourses(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            log.info("사용자 과목 조회 API 호출 - Authorization 헤더: {}", authorization);
            
            // JWT 토큰 검증
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님: {}", authorization);
                return ResponseEntity.status(401).body(null);
            }

            String token = authorization.substring(7); // "Bearer " 제거
            log.info("추출된 토큰: {}", token);
            
            // 토큰 유효성 검증
            if (!jwtService.validateToken(token)) {
                log.warn("토큰 유효성 검증 실패");
                return ResponseEntity.status(401).body(null);
            }
            
            String studentId = jwtService.getStudentIdFromToken(token);
            log.info("토큰에서 추출된 학번: {}", studentId);
            
            List<UserCourseDto> userCourses = userCourseService.getUserCoursesByToken(authorization);
            
            log.info("사용자 과목 조회 완료 - 총 {}개 과목", userCourses.size());
            return ResponseEntity.ok(userCourses);
            
        } catch (Exception e) {
            log.error("사용자 과목 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

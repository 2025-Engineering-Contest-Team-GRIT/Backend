package grit.guidance.domain.user.controller;

import grit.guidance.domain.user.dto.ErrorResponse;
import grit.guidance.domain.user.dto.LoginRequest;
import grit.guidance.domain.user.service.UsersCrawlingService;
import grit.guidance.domain.user.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "크롤링", description = "사용자 데이터 크롤링 관련 API")
public class CrawlingController {

    private final UsersCrawlingService crawlingService;
    private final LoginService loginService;

    @PostMapping("/crawling")
    @Operation(summary = "신규 사용자 데이터 크롤링", description = "신규 사용자의 약관 동의 후 한성대 포털에서 학사 데이터를 크롤링하여 저장")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "크롤링 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (학번/비밀번호 오류)"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<?> crawlUserData(@RequestBody LoginRequest request) {
        try {
            log.info("크롤링 요청 시작: studentId={}", request.studentId());
            
            // 크롤링 실행
            var hansungData = crawlingService.fetchHansungData(request.studentId(), request.password());
            
            // 크롤링된 데이터 저장/업데이트
            loginService.saveOrUpdateAllUserData(request.studentId(), hansungData);
            
            log.info("크롤링 및 데이터 저장 완료: studentId={}", request.studentId());
            
            return ResponseEntity.ok().body(new CrawlingResponse(200, "크롤링이 완료되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.warn("크롤링 실패 - 잘못된 인증: {}", e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse(400, e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("크롤링 중 오류 발생: {}", e.getMessage(), e);
            ErrorResponse errorResponse = new ErrorResponse(500, "크롤링 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // 크롤링 응답 DTO
    public record CrawlingResponse(
        Integer status,
        String message
    ) {}
}

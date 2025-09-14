package grit.guidance.domain.user.controller;

import grit.guidance.domain.user.dto.ErrorResponse;
import grit.guidance.domain.user.dto.LoginRequest;
import grit.guidance.domain.user.dto.LoginResponse;
import grit.guidance.domain.user.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 관련 API")
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "기존 사용자면 초기 화면 정보를 전달")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (사용자 이름/비밀번호 누락 또는 잘못된 비밀번호)"),
        @ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없습니다")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = loginService.login(request);
            return ResponseEntity.status(response.status()).body(response);
        } catch (IllegalArgumentException e) {
            // 400 Bad Request
            ErrorResponse errorResponse = new ErrorResponse(400, e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (RuntimeException e) {
            // 404 Not Found
            ErrorResponse errorResponse = new ErrorResponse(404, e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }
}

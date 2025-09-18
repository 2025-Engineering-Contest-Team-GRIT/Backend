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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

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
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            // 기존 쿠키 정리 (중복 방지)
            clearExistingCookies(response);
            
            LoginResponse loginResponse = loginService.login(request);
            return ResponseEntity.status(loginResponse.status()).body(loginResponse);
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

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃 처리")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<String> logout(HttpServletResponse response) {
        // 쿠키 기반 로그아웃: 토큰 쿠키를 무효화
        Cookie tokenCookie = new Cookie("token", "");
        tokenCookie.setMaxAge(0); // 즉시 만료
        tokenCookie.setPath("/"); // 전체 경로에서 유효
        tokenCookie.setHttpOnly(true); // XSS 공격 방지
        tokenCookie.setSecure(false); // 개발환경에서는 false, 운영환경에서는 true
        response.addCookie(tokenCookie);
        
        // access_token 쿠키도 무효화 (혹시 다른 이름으로 저장된 경우)
        Cookie accessTokenCookie = new Cookie("access_token", "");
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false);
        response.addCookie(accessTokenCookie);
        
        return ResponseEntity.status(204).body("로그아웃 성공");
    }
    
    /**
     * 기존 쿠키들을 정리하는 메서드
     */
    private void clearExistingCookies(HttpServletResponse response) {
        // 모든 가능한 토큰 쿠키명들 정리
        String[] cookieNames = {"token", "access_token", "jwt", "auth_token", "session"};
        
        for (String cookieName : cookieNames) {
            Cookie cookie = new Cookie(cookieName, "");
            cookie.setMaxAge(0);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            response.addCookie(cookie);
        }
    }
}

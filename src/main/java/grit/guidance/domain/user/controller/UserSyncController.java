package grit.guidance.domain.user.controller;

import grit.guidance.domain.user.dto.UserSyncRequestDto;
import grit.guidance.domain.user.service.UserAcademicInfoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserSyncController {

    private final UserAcademicInfoSyncService userAcademicInfoSyncService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncUserData(@RequestBody UserSyncRequestDto requestDto) {

        log.info("API 요청 수신: studentId='{}'", requestDto.studentId());

        try {
            userAcademicInfoSyncService.syncHansungInfo(requestDto.studentId(), requestDto.password());
            return ResponseEntity.ok(Map.of("message", "사용자 정보 동기화가 성공적으로 완료되었습니다."));
        } catch (Exception e) {
            // 크롤링 로그인 실패 등 서비스에서 발생한 예외를 처리
            return ResponseEntity.badRequest().body(Map.of("message", "사용자 정보 동기화에 실패했습니다.", "error", e.getMessage()));
        }
    }
}
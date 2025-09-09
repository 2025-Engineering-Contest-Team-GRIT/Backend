package grit.guidance.domain.course.controller;

import grit.guidance.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/courses") // 관리자용 API는 /api/admin 경로로 분리
@RequiredArgsConstructor
public class CourseAdminController {

    private final CourseService courseService;

    @PostMapping("/reload")
    // @PreAuthorize("hasRole('ADMIN')") // Spring Security 사용 시 이 어노테이션으로 권한 제어
    public ResponseEntity<Map<String, String>> reloadCourseData() {
        try {
            courseService.initializeDatabaseFromJson();
            return ResponseEntity.ok(Map.of("message", "과목 데이터 새로고침을 성공적으로 완료했습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "데이터 새로고침 중 오류가 발생했습니다.", "error", e.getMessage()));
        }
    }
}
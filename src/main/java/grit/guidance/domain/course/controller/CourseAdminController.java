package grit.guidance.domain.course.controller;

import grit.guidance.domain.course.service.CourseService;
import grit.guidance.domain.course.service.CourseDescriptionCrawlingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/courses") // 관리자용 API는 /api/admin 경로로 분리
@RequiredArgsConstructor
@Tag(name = "Course Admin", description = "과목 관리자 API")
public class CourseAdminController {

    private final CourseService courseService;
    private final CourseDescriptionCrawlingService crawlingService;

    @PostMapping("/reload")
    @Operation(summary = "과목 데이터 새로고침 및 설명 크롤링", description = "JSON 파일에서 과목 데이터를 읽어와 데이터베이스를 새로고침하고, 모든 과목의 설명을 크롤링하여 저장합니다.")
    // @PreAuthorize("hasRole('ADMIN')") // Spring Security 사용 시 이 어노테이션으로 권한 제어
    public ResponseEntity<Map<String, String>> reloadCourseData() {
        try {
            log.info("과목 데이터 새로고침 및 설명 크롤링 시작");
            
            // 1. JSON에서 과목 데이터 새로고침
            courseService.initializeDatabaseFromJson();
            log.info("과목 데이터 새로고침 완료");
            
            // 2. 모든 과목의 설명 크롤링
            crawlingService.updateCourseDescriptionsFromDatabase();
            log.info("과목 설명 크롤링 완료");
            
            return ResponseEntity.ok(Map.of("message", "과목 데이터 새로고침 및 설명 크롤링을 성공적으로 완료했습니다."));
        } catch (Exception e) {
            log.error("과목 데이터 새로고침 및 설명 크롤링 실패", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "데이터 새로고침 및 크롤링 중 오류가 발생했습니다.", "error", e.getMessage()));
        }
    }
}
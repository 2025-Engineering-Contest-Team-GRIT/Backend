package grit.guidance.domain.roadmap.controller;

import grit.guidance.domain.roadmap.service.CourseEmbeddingService;
import grit.guidance.domain.roadmap.repository.QdrantRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roadmap")
@Tag(name = "Roadmap", description = "로드맵 추천 API")
public class RoadmapController {

    private static final Logger log = LoggerFactory.getLogger(RoadmapController.class);
    
    private final CourseEmbeddingService courseEmbeddingService;
    private final QdrantRepository qdrantRepository;
    
    public RoadmapController(CourseEmbeddingService courseEmbeddingService, QdrantRepository qdrantRepository) {
        this.courseEmbeddingService = courseEmbeddingService;
        this.qdrantRepository = qdrantRepository;
    }

    @PostMapping("/courses/embed")
    @Operation(summary = "과목 데이터 벡터화 및 저장", description = "Course 테이블의 모든 과목을 Qdrant에 벡터화하여 저장합니다.")
    public ResponseEntity<Map<String, String>> embedAllCourses() {
        try {
            log.info("과목 데이터 벡터화 및 저장 요청 시작");

            courseEmbeddingService.embedAllCourses();

            return ResponseEntity.ok(Map.of(
                    "message", "과목 데이터를 성공적으로 Qdrant에 저장했습니다.",
                    "status", "success"
            ));

        } catch (Exception e) {
            log.error("과목 데이터 벡터화 및 저장 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "과목 데이터 저장 중 오류가 발생했습니다.",
                    "error", e.getMessage(),
                    "status", "error"
            ));
        }
    }

    @GetMapping("/courses/search")
    @Operation(summary = "과목 검색", description = "Qdrant에서 유사한 과목을 검색합니다.")
    public ResponseEntity<Map<String, Object>> searchCourses(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        try {
            log.info("과목 검색 요청: {} (상위 {}개)", query, topK);

            List<Map<String, Object>> results = courseEmbeddingService.searchCourses(query, topK);

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("topK", topK);
            response.put("results", results);
            response.put("count", results.size());
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("과목 검색 실패: {}", query, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "과목 검색 중 오류가 발생했습니다.",
                    "error", e.getMessage(),
                    "status", "error"
            ));
        }
    }

    @GetMapping("/courses/health")
    @Operation(summary = "Qdrant 상태 확인", description = "Qdrant 벡터 스토어의 상태를 확인합니다.")
    public ResponseEntity<Map<String, Object>> checkQdrantHealth() {
        try {
            boolean isHealthy = courseEmbeddingService.isQdrantHealthy();
            long documentCount = courseEmbeddingService.getStoredCourseCount();

            Map<String, Object> response = new HashMap<>();
            response.put("isHealthy", isHealthy);
            response.put("documentCount", documentCount);
            response.put("status", isHealthy ? "healthy" : "unhealthy");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Qdrant 상태 확인 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Qdrant 상태 확인 중 오류가 발생했습니다.",
                    "error", e.getMessage(),
                    "status", "error"
            ));
        }
    }

    @DeleteMapping("/courses/clear")
    @Operation(summary = "Qdrant 데이터 초기화", description = "Qdrant에 저장된 모든 과목 데이터를 삭제합니다.")
    public ResponseEntity<Map<String, String>> clearAllCourses() {
        try {
            log.info("Qdrant 데이터 초기화 요청");

            boolean success = qdrantRepository.deleteAllCourseDocuments();

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "message", "Qdrant 데이터를 성공적으로 초기화했습니다.",
                        "status", "success"
                ));
            } else {
                return ResponseEntity.internalServerError().body(Map.of(
                        "message", "Qdrant 데이터 초기화에 실패했습니다.",
                        "status", "error"
                ));
            }

        } catch (Exception e) {
            log.error("Qdrant 데이터 초기화 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Qdrant 데이터 초기화 중 오류가 발생했습니다.",
                    "error", e.getMessage(),
                    "status", "error"
            ));
        }
    }
}
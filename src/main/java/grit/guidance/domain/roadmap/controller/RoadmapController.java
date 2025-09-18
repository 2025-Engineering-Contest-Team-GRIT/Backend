package grit.guidance.domain.roadmap.controller;

import grit.guidance.domain.roadmap.service.CourseEmbeddingService;
import grit.guidance.domain.roadmap.repository.QdrantRepository;
import grit.guidance.domain.roadmap.dto.SearchRequest;
import grit.guidance.domain.roadmap.dto.CourseRecommendationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/roadmap")
@Tag(name = "Roadmap", description = "로드맵 추천 API")
public class RoadmapController {

    private static final Logger log = LoggerFactory.getLogger(RoadmapController.class);
    
    private final CourseEmbeddingService courseEmbeddingService;
    private final QdrantRepository qdrantRepository;

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

    @PostMapping("/courses/search")
    @Operation(summary = "과목 검색", description = "Qdrant에서 유사한 과목을 검색합니다.")
    public ResponseEntity<Map<String, Object>> searchCourses(@RequestBody SearchRequest request) {
        try {
            int topK = 20;  // 고정값으로 20개 설정
            log.info("RoadmapController 검색 요청: query='{}', topK={}", request.getQuery(), topK);

            List<Map<String, Object>> results = courseEmbeddingService.searchCoursesByPreference(request.getQuery(), topK);

            Map<String, Object> response = new HashMap<>();
            response.put("query", request.getQuery());
            response.put("topK", topK);
            response.put("results", results);
            response.put("count", results.size());
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("과목 검색 실패: {}", request.getQuery(), e);
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

    @PostMapping("/courses/recommend")
    @Operation(summary = "과목 추천", description = "사용자의 트랙과 학습 스타일에 따라 과목을 추천합니다.")
    public ResponseEntity<Map<String, Object>> recommendCourses(@RequestBody CourseRecommendationRequest request) {
        try {
            log.info("과목 추천 요청: studentId={}, trackIds={}, learningStyle={}, advancedSettings={}",
                    request.getStudentId(), request.getTrackIds(), request.getLearningStyle(), request.getAdvancedSettings());

            // 1단계: 필수 과목 목록 확보 (규칙 기반 필터링)
            List<Map<String, Object>> mandatoryCourses = courseEmbeddingService.getMandatoryCourses(request.getTrackIds(), request.getStudentId());
            
            // 2단계: 벡터 DB 검색 목록 확보 (유사도 검색)
            List<Map<String, Object>> recommendedCourses = courseEmbeddingService.getRecommendedCourses(
                    request.getTrackIds(), request.getStudentId(), request.getLearningStyle(), request.getAdvancedSettings());

            Map<String, Object> response = new HashMap<>();
            response.put("studentId", request.getStudentId());
            response.put("trackIds", request.getTrackIds());
            response.put("learningStyle", request.getLearningStyle());
            response.put("advancedSettings", request.getAdvancedSettings());
            response.put("mandatoryCourses", mandatoryCourses);
            response.put("recommendedCourses", recommendedCourses);
            response.put("mandatoryCount", mandatoryCourses.size());
            response.put("recommendedCount", recommendedCourses.size());
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("과목 추천 실패: trackIds={}, learningStyle={}", request.getTrackIds(), request.getLearningStyle(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "과목 추천 중 오류가 발생했습니다.",
                    "error", e.getMessage(),
                    "status", "error"
            ));
        }
    }

    @PostMapping("/courses/roadmap")
    @Operation(summary = "통합 로드맵 추천", description = "1단계 필수과목 + 2단계 유사도검색 + LLM 로드맵 추천을 통합합니다.")
    public ResponseEntity<Map<String, Object>> recommendRoadmap(@RequestBody CourseRecommendationRequest request) {
        try {
            log.info("통합 로드맵 추천 요청: studentId={}, trackIds={}, learningStyle={}, advancedSettings={}",
                    request.getStudentId(), request.getTrackIds(), request.getLearningStyle(), request.getAdvancedSettings());

            // 통합 로드맵 추천 (1단계 + 2단계 + LLM)
            Map<String, Object> response = courseEmbeddingService.getIntegratedRoadmapRecommendation(
                    request.getTrackIds(), request.getStudentId(), request.getLearningStyle(), request.getAdvancedSettings());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("통합 로드맵 추천 실패: trackIds={}, learningStyle={}", request.getTrackIds(), request.getLearningStyle(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "통합 로드맵 추천 중 오류가 발생했습니다.",
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
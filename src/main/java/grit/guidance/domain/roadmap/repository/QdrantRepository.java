package grit.guidance.domain.roadmap.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Repository
public class QdrantRepository {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String qdrantHost;
    private final int qdrantPort;
    private final String collectionName;

    public QdrantRepository(@Value("${spring.ai.vectorstore.qdrant.host:localhost}") String qdrantHost,
                           @Value("${spring.ai.vectorstore.qdrant.port:6333}") int qdrantPort,
                           @Value("${spring.ai.vectorstore.qdrant.collection:test_collection}") String collectionName) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.qdrantHost = qdrantHost;
        this.qdrantPort = qdrantPort;
        this.collectionName = collectionName;
    }

    /**
     * OpenAI API를 사용하여 텍스트를 벡터로 변환
     */
    private List<Double> generateEmbedding(String text) {
        try {
            // 간단한 더미 벡터 생성 (실제로는 OpenAI API 호출)
            // TODO: 실제 OpenAI API 호출로 교체
            List<Double> vector = new ArrayList<>();
            for (int i = 0; i < 1536; i++) {
                vector.add(Math.random());
            }
            return vector;
        } catch (Exception e) {
            System.err.println("벡터 생성 실패: " + e.getMessage());
            // 기본 벡터 반환
            List<Double> vector = new ArrayList<>();
            for (int i = 0; i < 1536; i++) {
                vector.add(0.0);
            }
            return vector;
        }
    }

    /**
     * 과목 문서를 Qdrant에 직접 저장
     */
    public void addCourseDocument(Map<String, Object> document) {
        try {
            String description = (String) document.get("description");
            String id = (String) document.get("id");
            
            // OpenAI API로 벡터 생성 (description 사용)
            List<Double> vector = generateEmbedding(description);
            
            // Qdrant 포인트 생성 (ID를 숫자로 변환)
            int numericId = Math.abs(id.hashCode());
            Map<String, Object> point = new HashMap<>();
            point.put("id", numericId);
            point.put("vector", vector);
            point.put("payload", document);
            
            // Qdrant API로 저장
            String url = String.format("http://%s:%d/collections/%s/points", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", List.of(point));
            requestBody.put("ids", List.of(numericId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Qdrant 직접 저장 성공: " + id);
            } else {
                System.err.println("Qdrant 저장 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("Qdrant 직접 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 여러 과목 문서를 벡터 스토어에 일괄 저장
     */
    public void addCourseDocuments(List<Map<String, Object>> documents) {
        try {
            List<Map<String, Object>> points = new ArrayList<>();
            
            List<Integer> ids = new ArrayList<>();
            
            for (Map<String, Object> document : documents) {
                String description = (String) document.get("description");
                String id = (String) document.get("id");
                
                // OpenAI API로 벡터 생성 (description 사용)
                List<Double> vector = generateEmbedding(description);
                
                // Qdrant 포인트 생성 (ID를 숫자로 변환)
                int numericId = Math.abs(id.hashCode());
                Map<String, Object> point = new HashMap<>();
                point.put("id", numericId);
                point.put("vector", vector);
                point.put("payload", document);
                
                points.add(point);
                ids.add(numericId);
            }
            
            // Qdrant API로 일괄 저장
            String url = String.format("http://%s:%d/collections/%s/points", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", points);
            requestBody.put("ids", ids);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Qdrant 직접 일괄 저장 성공: " + documents.size() + "개 문서");
            } else {
                System.err.println("Qdrant 일괄 저장 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            System.err.println("Qdrant 직접 일괄 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 유사한 과목을 검색 (기본 5개)
     */
    public List<Map<String, Object>> searchSimilarCourses(String query) {
        return searchSimilarCourses(query, 5);
    }

    /**
     * 유사한 과목을 검색 (개수 지정)
     */
    public List<Map<String, Object>> searchSimilarCourses(String query, int topK) {
        try {
            // 쿼리 텍스트를 벡터로 변환
            List<Double> queryVector = generateEmbedding(query);
            
            // Qdrant 검색 API 호출
            String url = String.format("http://%s:%d/collections/%s/points/search", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vector", queryVector);
            requestBody.put("limit", topK);
            requestBody.put("with_payload", true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("result");
                
                return results.stream()
                        .map(result -> {
                            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                            Map<String, Object> searchResult = new HashMap<>(payload);
                            searchResult.put("score", result.get("score"));
                            return searchResult;
                        })
                        .toList();
            } else {
                System.err.println("Qdrant 검색 실패: " + response.getStatusCode());
                return List.of();
            }
                    
        } catch (Exception e) {
            System.err.println("Qdrant 직접 검색 실패: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 유사한 과목을 검색 (임계값 지정)
     */
    public List<Map<String, Object>> searchSimilarCourses(String query, int topK, double threshold) {
        // 임계값 검색은 기본 검색으로 대체
        return searchSimilarCourses(query, topK);
    }

    /**
     * 필터를 사용한 과목 검색
     */
    public List<Map<String, Object>> searchCoursesWithFilter(String query, int topK, String filterExpression) {
        // 필터 검색은 기본 검색으로 대체
        return searchSimilarCourses(query, topK);
    }

    /**
     * 특정 과목 문서 삭제 (ID 기반)
     */
    public boolean deleteCourseDocument(String courseId) {
        try {
            String url = String.format("http://%s:%d/collections/%s/points/delete", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", List.of(courseId));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Qdrant 직접 문서 삭제 성공: " + courseId);
                return true;
            } else {
                System.err.println("Qdrant 문서 삭제 실패: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Qdrant 직접 문서 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 여러 과목 문서 삭제
     */
    public boolean deleteCourseDocuments(List<String> courseIds) {
        try {
            String url = String.format("http://%s:%d/collections/%s/points/delete", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", courseIds);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Qdrant 직접 " + courseIds.size() + "개 문서 삭제 성공");
                return true;
            } else {
                System.err.println("Qdrant 일괄 삭제 실패: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Qdrant 직접 일괄 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 모든 과목 문서 삭제 (개발/테스트용)
     */
    public boolean deleteAllCourseDocuments() {
        try {
            String url = String.format("http://%s:%d/collections/%s/points/delete", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filter", Map.of("must", List.of()));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Qdrant 직접 전체 삭제 성공");
                return true;
            } else {
                System.err.println("Qdrant 전체 삭제 실패: " + response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Qdrant 직접 전체 삭제 실패: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 벡터 스토어 상태 확인
     */
    public boolean isVectorStoreHealthy() {
        try {
            String url = String.format("http://%s:%d/collections/%s", qdrantHost, qdrantPort, collectionName);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("Qdrant 상태 확인 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 저장된 문서 개수 확인
     */
    public long getDocumentCount() {
        try {
            String url = String.format("http://%s:%d/collections/%s", qdrantHost, qdrantPort, collectionName);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get("result");
                if (result != null && result.containsKey("points_count")) {
                    return ((Number) result.get("points_count")).longValue();
                }
            }
            return 0L;
        } catch (Exception e) {
            System.err.println("Qdrant 문서 개수 확인 실패: " + e.getMessage());
            return -1L;
        }
    }
}

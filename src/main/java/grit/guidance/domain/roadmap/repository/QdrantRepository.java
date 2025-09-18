package grit.guidance.domain.roadmap.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
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
    private final String openaiApiKey;

    public QdrantRepository(@Value("${spring.ai.vectorstore.qdrant.host:localhost}") String qdrantHost,
                           @Value("${spring.ai.vectorstore.qdrant.port:6333}") int qdrantPort,
                           @Value("${spring.ai.vectorstore.qdrant.collection:test_collection}") String collectionName,
                           @Value("${spring.ai.openai.api-key:}") String openaiApiKey) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.qdrantHost = qdrantHost;
        this.qdrantPort = qdrantPort;
        this.collectionName = collectionName;
        
        // .env 파일에서 직접 API 키 읽기
        String envApiKey = null;
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            envApiKey = dotenv.get("OPENAI_API_KEY");
        } catch (Exception e) {
            System.out.println(".env 파일을 읽을 수 없습니다: " + e.getMessage());
        }
        
        // Spring 설정에서 읽은 키가 있으면 사용, 없으면 .env에서 읽은 키 사용
        this.openaiApiKey = (openaiApiKey != null && !openaiApiKey.trim().isEmpty()) ? openaiApiKey : envApiKey;
        
        // 디버깅용 로그
        System.out.println("QdrantRepository 초기화:");
        System.out.println("  - qdrantHost: " + qdrantHost);
        System.out.println("  - qdrantPort: " + qdrantPort);
        System.out.println("  - collectionName: " + collectionName);
        System.out.println("  - Spring API Key: " + (openaiApiKey != null ? openaiApiKey.substring(0, Math.min(20, openaiApiKey.length())) + "..." : "null"));
        System.out.println("  - .env API Key: " + (envApiKey != null ? envApiKey.substring(0, Math.min(20, envApiKey.length())) + "..." : "null"));
        System.out.println("  - Final API Key: " + (this.openaiApiKey != null ? this.openaiApiKey.substring(0, Math.min(20, this.openaiApiKey.length())) + "..." : "null"));
    }

    /**
     * OpenAI API를 사용하여 텍스트를 벡터로 변환
     */
    private List<Double> generateEmbedding(String text) {
        try {
            // OpenAI API 키가 없으면 더미 벡터 사용
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                System.out.println("OpenAI API 키가 설정되지 않아 더미 벡터를 사용합니다.");
                return generateDummyVector();
            }

            // OpenAI API 호출
            String url = "https://api.openai.com/v1/embeddings";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", "text-embedding-ada-002");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                if (data != null && !data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    System.out.println("OpenAI API로 벡터 생성 성공: " + text.substring(0, Math.min(50, text.length())) + "...");
                    return embedding;
                }
            }
            
            System.err.println("OpenAI API 응답 오류: " + response.getStatusCode());
            return generateDummyVector();
            
        } catch (Exception e) {
            System.err.println("OpenAI API 호출 실패: " + e.getMessage());
            return generateDummyVector();
        }
    }

    /**
     * 더미 벡터 생성 (OpenAI API 사용 불가 시)
     */
    private List<Double> generateDummyVector() {
        List<Double> vector = new ArrayList<>();
        for (int i = 0; i < 1536; i++) {
            vector.add(Math.random());
        }
        return vector;
    }

    /**
     * 문서에서 벡터화할 텍스트 생성
     */
    private String createEmbeddingText(Map<String, Object> document) {
        StringBuilder text = new StringBuilder();

        // 과목명
        String courseName = (String) document.get("courseName");
        if (courseName != null) {
            text.append(courseName).append(" ");
        }

        // 과목 설명
        String description = (String) document.get("description");
        if (description != null && !description.trim().isEmpty()) {
            text.append(description).append(" ");
        }

        // 과목 코드
        String courseCode = (String) document.get("courseCode");
        if (courseCode != null) {
            text.append(courseCode).append(" ");
        }

        // 학점 정보
        Object credits = document.get("credits");
        if (credits != null) {
            text.append(credits).append("학점 ");
        }

        // 개설 학년
        Object openGrade = document.get("openGrade");
        if (openGrade != null) {
            text.append(openGrade).append("학년 ");
        }

        // 개설 학기
        String openSemester = (String) document.get("openSemester");
        if (openSemester != null) {
            text.append(openSemester).append(" ");
        }

        // 트랙명 (트랙 요구사항인 경우)
        String trackName = (String) document.get("trackName");
        if (trackName != null) {
            text.append(trackName).append(" ");
        }

        // 과목 타입 (트랙 요구사항인 경우)
        String courseTypeDescription = (String) document.get("courseTypeDescription");
        if (courseTypeDescription != null) {
            text.append(courseTypeDescription).append(" ");
        }

        return text.toString().trim();
    }

    /**
     * 과목 문서를 Qdrant에 직접 저장
     */
    public void addCourseDocument(Map<String, Object> document) {
        try {
            String id = (String) document.get("id");
            
            // 벡터화할 텍스트 생성 (과목명 + 설명 + 기타 정보)
            String text = createEmbeddingText(document);
            
            // OpenAI API로 벡터 생성
            List<Double> vector = generateEmbedding(text);
            
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
                System.out.println("Qdrant 직접 저장 성공: " + id + " - " + text.substring(0, Math.min(50, text.length())));
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
     * 유사한 과목을 검색 (개수 지정)
     */
    public List<Map<String, Object>> searchSimilarCourses(String query, int topK) {
        try {
            System.out.println("🔍 Qdrant 검색 시작: query='" + query + "', topK=" + topK);
            
            // 쿼리 텍스트를 벡터로 변환
            List<Double> queryVector = generateEmbedding(query);
            
            // Qdrant 검색 API 호출
            String url = String.format("http://%s:%d/collections/%s/points/search", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vector", queryVector);
            requestBody.put("limit", topK);
            requestBody.put("with_payload", true);
            
            System.out.println("Qdrant 요청 본문: " + objectMapper.writeValueAsString(requestBody));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("result");
                
                System.out.println("Qdrant 검색 성공: " + (results != null ? results.size() : 0) + "개 결과 반환");
                
                if (results != null) {
                    return results.stream()
                            .map(result -> {
                                Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                                Map<String, Object> searchResult = new HashMap<>(payload);
                                searchResult.put("score", result.get("score"));
                                // text 필드 제거 (description과 중복)
                                searchResult.remove("text");
                                return searchResult;
                            })
                            .toList();
                } else {
                    System.out.println("Qdrant 검색 결과가 null입니다.");
                    return List.of();
                }
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
     * 트랙 필터링을 포함한 유사도 검색
     */
    public List<Map<String, Object>> searchSimilarCoursesWithFilter(String query, int topK, List<String> trackNames) {
        try {
            System.out.println("🔍 Qdrant 필터링 검색 시작: query='" + query + "', topK=" + topK + ", trackNames=" + trackNames);
            
            // 쿼리 텍스트를 벡터로 변환
            List<Double> queryVector = generateEmbedding(query);
            
            // Qdrant 검색 API 호출
            String url = String.format("http://%s:%d/collections/%s/points/search", qdrantHost, qdrantPort, collectionName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("vector", queryVector);
            requestBody.put("limit", topK);
            requestBody.put("with_payload", true);
            
            // 트랙 필터링 조건 추가
            if (trackNames != null && !trackNames.isEmpty()) {
                Map<String, Object> filter = new HashMap<>();
                Map<String, Object> must = new HashMap<>();
                Map<String, Object> key = new HashMap<>();
                key.put("key", "tracks");
                key.put("match", Map.of("any", trackNames));
                must.put("must", List.of(key));
                filter.put("must", List.of(must));
                requestBody.put("filter", filter);
            }
            
            System.out.println("Qdrant 필터링 요청 본문: " + objectMapper.writeValueAsString(requestBody));
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("result");
                
                System.out.println("Qdrant 필터링 검색 성공: " + (results != null ? results.size() : 0) + "개 결과 반환");
                
                if (results != null) {
                    return results.stream()
                            .map(result -> {
                                Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                                Map<String, Object> searchResult = new HashMap<>(payload);
                                searchResult.put("score", result.get("score"));
                                // text 필드 제거 (description과 중복)
                                searchResult.remove("text");
                                return searchResult;
                            })
                            .toList();
                } else {
                    System.out.println("Qdrant 필터링 검색 결과가 null입니다.");
                    return List.of();
                }
            } else {
                System.err.println("Qdrant 필터링 검색 실패: " + response.getStatusCode());
                return List.of();
            }
                    
        } catch (Exception e) {
            System.err.println("Qdrant 필터링 검색 실패: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
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

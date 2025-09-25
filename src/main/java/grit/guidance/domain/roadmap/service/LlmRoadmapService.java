package grit.guidance.domain.roadmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmRoadmapService {

    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.url}")
    private String openaiApiUrl;

    /**
     * RestTemplate을 매번 새로 생성하여 쿠키 충돌 방지
     */
    private RestTemplate createFreshRestTemplate() {
        CookieStore cookieStore = new BasicCookieStore();
        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .disableRedirectHandling()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(requestFactory);
    }

    /**
     * LLM에게 로드맵 추천 요청
     */
    public Map<String, Object> generateRoadmapRecommendation(
            String studentId,
            List<Long> trackIds,
            List<Map<String, Object>> mandatoryCourses,
            List<Map<String, Object>> recommendedCourses,
            String techStack,
            Map<String, Object> semesterInfo) {
        
        try {
            log.info("LLM 로드맵 추천 요청 시작 - studentId: {}, trackIds: {}, mandatory: {}개, recommended: {}개",
                    studentId, trackIds, mandatoryCourses.size(), recommendedCourses.size());

            // LLM에게 보낼 데이터 상세 로그
            log.info("LLM에게 보낼 필수 과목 목록 ({}개):", mandatoryCourses.size());
            for (int i = 0; i < mandatoryCourses.size(); i++) {
                Map<String, Object> course = mandatoryCourses.get(i);
                log.info("  {}. {} ({}) - {} [{}학년 {}학기]", 
                    i + 1,
                    course.get("courseName"),
                    course.get("courseCode"),
                    course.get("courseType"),
                    course.get("openGrade"),
                    course.get("openSemester"));
            }
            
            log.info("LLM에게 보낼 추천 과목 목록 ({}개):", recommendedCourses.size());
            for (int i = 0; i < recommendedCourses.size(); i++) {
                Map<String, Object> course = recommendedCourses.get(i);
                log.info("  {}. {} ({}) - {}학년 {}학기 - 유사도: {}", 
                    i + 1,
                    course.get("courseName"),
                    course.get("courseCode"),
                    course.get("openGrade"),
                    course.get("openSemester"),
                    course.get("score"));
            }

            // 프롬프트 생성 (학기 정보 포함)
            String prompt = buildPrompt(mandatoryCourses, recommendedCourses, techStack, semesterInfo);
            
            // OpenAI API 호출
            Map<String, Object> response = callOpenAI(prompt);
            
            log.info("LLM 로드맵 추천 완료");
            return response;

        } catch (Exception e) {
            log.error("LLM 로드맵 추천 실패", e);
            throw new RuntimeException("로드맵 추천 생성에 실패했습니다.", e);
        }
    }

    /**
     * 프롬프트 생성
     */
    private String buildPrompt(List<Map<String, Object>> mandatoryCourses, 
                              List<Map<String, Object>> recommendedCourses, 
                              String techStack,
                              Map<String, Object> semesterInfo) {
        
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("당신은 한성대학교 컴퓨터공학부 학생들을 위한 최고의 학업 로드맵 설계 AI입니다. ");
        prompt.append("당신의 임무는 주어진 학생 정보와 추천 과목 목록, 그리고 절대 규칙을 바탕으로, ");
        prompt.append("학생의 성공적인 미래를 위한 최적의 학기별 수강 계획을 짜주는 것입니다.\n\n");
        
        // 학생 학기 정보 추가
        if (semesterInfo != null) {
            int latestYear = (Integer) semesterInfo.get("latestCompletedYear");
            String latestSemester = (String) semesterInfo.get("latestCompletedSemester");
            boolean hasCurrentEnrollment = (Boolean) semesterInfo.get("hasCurrentEnrollment");
            int nextYear = (Integer) semesterInfo.get("nextYear");
            String nextSemester = (String) semesterInfo.get("nextSemester");
            
            prompt.append("**학생 학기 정보:**\n");
            prompt.append(String.format("- 최신 이수 학기: %d학년 %s학기\n", latestYear, latestSemester));
            prompt.append(String.format("- 현재 수강중: %s\n", hasCurrentEnrollment ? "예" : "아니오"));
            prompt.append(String.format("- 추천 시작 학기: %d학년 %s학기부터\n\n", nextYear, nextSemester));
        }
        
        prompt.append("아래 목록의 과목들을 활용하여 로드맵을 구성하세요. ");
        prompt.append("목록은 '필수 과목'과 '추천 과목'으로 나뉩니다.\n\n");
        
        // 1. 필수 과목 목록
        prompt.append("1. 필수 과목 목록 (반드시 로드맵에 포함되어야 함)\n");
        if (mandatoryCourses.isEmpty()) {
            prompt.append("없음\n");
        } else {
            for (Map<String, Object> course : mandatoryCourses) {
                prompt.append(String.format("- %s (%s): %s - %s [개설: %d학년 %s학기]\n", 
                    course.get("courseName") != null ? course.get("courseName") : "N/A", 
                    course.get("courseCode") != null ? course.get("courseCode") : "N/A",
                    course.get("courseType") != null ? course.get("courseType") : "N/A",
                    course.get("description") != null ? course.get("description") : "N/A",
                    course.get("openGrade") != null ? course.get("openGrade") : "N/A",
                    course.get("openSemester") != null ? course.get("openSemester") : "N/A"));
            }
        }
        
        prompt.append("\n");
        
        // 2. 추천 과목 목록
        prompt.append("2. 추천 과목 목록 (학생의 선호도에 따라 선택적으로 포함)\n");
        if (techStack != null && !techStack.trim().isEmpty()) {
            prompt.append(String.format("학생의 관심 기술 스택: %s\n", techStack));
        }
        
        if (recommendedCourses.isEmpty()) {
            prompt.append("없음\n");
        } else {
            for (Map<String, Object> course : recommendedCourses) {
                // score 값 안전하게 처리
                Object scoreObj = course.get("score");
                String scoreStr = "N/A";
                if (scoreObj instanceof Number) {
                    scoreStr = String.format("%.3f", ((Number) scoreObj).doubleValue());
                } else if (scoreObj != null) {
                    scoreStr = scoreObj.toString();
                }
                
                prompt.append(String.format("- %s (%s): %s - %s [개설: %d학년 %s학기] (유사도: %s)\n", 
                    course.get("courseName") != null ? course.get("courseName") : "N/A", 
                    course.get("courseCode") != null ? course.get("courseCode") : "N/A",
                    course.get("description") != null ? course.get("description") : "N/A",
                    course.get("tracks") != null ? course.get("tracks") : "N/A",
                    course.get("openGrade") != null ? course.get("openGrade") : "N/A",
                    course.get("openSemester") != null ? course.get("openSemester") : "N/A",
                    scoreStr));
            }
        }
        
        prompt.append("\n");
        
        // 3. JSON 형식 요구사항
        prompt.append("결과는 반드시 아래와 같은 JSON 형식으로만 응답해야 합니다. ");
        prompt.append("roadMap 배열 안에는 학기별로 추천 과목을 그룹화하고, ");
        prompt.append("각 과목에는 추천 이유(recommendDescription)를 반드시 포함해야 합니다.\n");
        prompt.append("**중요 규칙:**\n");
        prompt.append("1. 추천 시작 학기부터 순차적으로 로드맵을 구성하세요.\n");
        prompt.append("2. 각 과목은 해당 과목의 개설 학년/학기에만 추천할 수 있습니다.\n");
        prompt.append("3. 예: 3학년 2학기 과목은 3학년 2학기에만 추천 가능합니다.\n");
        prompt.append("4. 캡스톤 과목은 하나만 추천하세요.\n");
        prompt.append("5. (ipp)과목은 추천하지 마세요.\n\n");
        
        prompt.append("{\n");
        prompt.append("  \"roadMap\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"recommendYear\": 4,\n");
        prompt.append("      \"recommendSemester\": \"SECOND\",\n");
        prompt.append("      \"courses\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"courseCode\": \"과목코드\",\n");
        prompt.append("          \"courseName\": \"과목이름\",\n");
        prompt.append("          \"recommendDescription\": \"추천이유\"\n");
        prompt.append("        },\n");
        prompt.append("        {\n");
        prompt.append("          \"courseCode\": \"과목코드\",\n");
        prompt.append("          \"courseName\": \"과목이름\",\n");
        prompt.append("          \"recommendDescription\": \"추천이유\"\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    /**
     * OpenAI API 호출
     */
    private Map<String, Object> callOpenAI(String prompt) throws Exception {
        String url = openaiApiUrl + "/v1/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o");
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2000);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + openaiApiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        log.info("OpenAI API 호출 시작");
        RestTemplate restTemplate = createFreshRestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                String content = (String) message.get("content");
                
                log.info("OpenAI API 응답 성공");
                log.info("LLM 응답 내용: {}", content);
                
                // JSON 추출 (마크다운 코드 블록에서 JSON 부분만 추출)
                String jsonContent = extractJsonFromContent(content);
                log.info("추출된 JSON: {}", jsonContent);
                
                // JSON 파싱
                return objectMapper.readValue(jsonContent, Map.class);
            }
        }
        
        throw new RuntimeException("OpenAI API 호출 실패: " + response.getStatusCode());
    }

    /**
     * LLM 응답에서 JSON 부분만 추출
     */
    private String extractJsonFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("LLM 응답이 비어있습니다.");
        }

        // 마크다운 코드 블록에서 JSON 추출
        String[] lines = content.split("\n");
        StringBuilder jsonBuilder = new StringBuilder();
        boolean inJsonBlock = false;
        int braceCount = 0;
        boolean foundFirstBrace = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // JSON 블록 시작 감지 (```json 또는 ```)
            if (trimmedLine.startsWith("```json") || trimmedLine.startsWith("```")) {
                inJsonBlock = true;
                continue;
            }
            
            // JSON 블록 종료 감지
            if (inJsonBlock && trimmedLine.equals("```")) {
                break;
            }
            
            // JSON 블록 내부에서 JSON 추출
            if (inJsonBlock) {
                // 첫 번째 { 찾기
                if (!foundFirstBrace && trimmedLine.contains("{")) {
                    foundFirstBrace = true;
                }
                
                if (foundFirstBrace) {
                    jsonBuilder.append(line).append("\n");
                    
                    // 중괄호 카운팅으로 JSON 완성도 확인
                    for (char c : line.toCharArray()) {
                        if (c == '{') braceCount++;
                        if (c == '}') braceCount--;
                    }
                    
                    // JSON이 완성되면 중단
                    if (braceCount == 0 && foundFirstBrace) {
                        break;
                    }
                }
            }
        }

        String jsonContent = jsonBuilder.toString().trim();
        
        if (jsonContent.isEmpty()) {
            // 마크다운 코드 블록이 없는 경우, 전체 내용에서 JSON 추출 시도
            jsonContent = extractJsonFromPlainText(content);
        }
        
        if (jsonContent.isEmpty()) {
            throw new RuntimeException("LLM 응답에서 유효한 JSON을 찾을 수 없습니다. 응답: " + content);
        }
        
        return jsonContent;
    }

    /**
     * 일반 텍스트에서 JSON 추출
     */
    private String extractJsonFromPlainText(String content) {
        int startIndex = content.indexOf('{');
        if (startIndex == -1) {
            return "";
        }
        
        int braceCount = 0;
        int endIndex = startIndex;
        
        for (int i = startIndex; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            
            if (braceCount == 0) {
                endIndex = i + 1;
                break;
            }
        }
        
        return content.substring(startIndex, endIndex);
    }
}

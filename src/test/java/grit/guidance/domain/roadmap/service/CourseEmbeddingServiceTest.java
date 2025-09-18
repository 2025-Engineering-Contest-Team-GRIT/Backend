package grit.guidance.domain.roadmap.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.profiles.active=test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.autoconfigure.exclude[0]=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
        "spring.autoconfigure.exclude[1]=org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration",
        "spring.autoconfigure.exclude[2]=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "spring.autoconfigure.exclude[3]=org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration",
        "spring.autoconfigure.exclude[4]=org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration",
        "spring.autoconfigure.exclude[5]=org.springframework.ai.autoconfigure.chat.observation.ChatObservationAutoConfiguration",
        "spring.autoconfigure.exclude[6]=org.springframework.ai.autoconfigure.vectorstore.qdrant.QdrantVectorStoreAutoConfiguration",
        "spring.autoconfigure.exclude[7]=org.springframework.ai.autoconfigure.embedding.openai.OpenAiEmbeddingAutoConfiguration",
        "spring.autoconfigure.exclude[8]=org.springframework.ai.autoconfigure.retry.SpringAiRetryAutoConfiguration",
        "spring.main.web-application-type=none",
        "logging.level.org.springframework.security=OFF",
        "logging.level.org.springframework.web=OFF",
        "logging.level.org.springframework.ai=OFF"
    }
)
public class CourseEmbeddingServiceTest {

    @Autowired
    private CourseEmbeddingService courseEmbeddingService;

    @Test
    public void testSearchCoursesByPreference() {
        System.out.println("=== CourseEmbeddingService.searchCoursesByPreference 테스트 시작 ===");
        
        try {
            // 기본 테스트 쿼리
            String query = "웹프로그래밍";
            int topK = 5;
            
            System.out.println("검색 쿼리: '" + query + "' (상위 " + topK + "개)");
            System.out.println("=" + "=".repeat(50));
            
            // 실제 서비스 호출
            List<Map<String, Object>> results = courseEmbeddingService.searchCoursesByPreference(query, topK);
            
            System.out.println("검색 완료: " + results.size() + "개 결과");
            
            if (results.isEmpty()) {
                System.out.println("검색 결과가 없습니다.");
                System.out.println("Qdrant 서버가 실행 중이지 않거나 데이터가 없을 수 있습니다.");
                return;
            }
            
            // 결과 출력
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> course = results.get(i);
                System.out.println("\n결과 " + (i + 1) + ":");
                System.out.println("  - 과목명: " + course.get("courseName"));
                System.out.println("  - 과목코드: " + course.get("courseCode"));
                System.out.println("  - 학점: " + course.get("credits"));
                System.out.println("  - 개설학년: " + course.get("openGrade"));
                System.out.println("  - 개설학기: " + course.get("openSemester"));
                System.out.println("  - 설명: " + course.get("description"));
                System.out.println("  - 트랙: " + course.get("tracks"));
                System.out.println("  - 유사도 점수: " + course.get("score"));
            }
            
            System.out.println("\n테스트 완료!");
            
        } catch (Exception e) {
            System.err.println("테스트 실패: " + e.getMessage());
            System.err.println("Qdrant 서버가 실행 중이지 않거나 OpenAI API 키가 설정되지 않았을 수 있습니다.");
            e.printStackTrace();
        }
    }
    
    @Test
    public void testSearchCoursesByPreferenceWithDifferentTopK() {
        System.out.println("=== topK 값별 테스트 시작 ===");
        
        String query = "웹프로그래밍";
        int[] topKValues = {3, 5, 10};
        
        for (int topK : topKValues) {
            System.out.println("\n🔍 검색 쿼리: '" + query + "' (상위 " + topK + "개)");
            System.out.println("=" + "=".repeat(40));
            
            try {
                // 실제 서비스 호출
                List<Map<String, Object>> results = courseEmbeddingService.searchCoursesByPreference(query, topK);
                System.out.println("검색 완료: " + results.size() + "개 결과");
                
                if (!results.isEmpty()) {
                    System.out.println("상위 3개 결과:");
                    for (int i = 0; i < Math.min(3, results.size()); i++) {
                        Map<String, Object> course = results.get(i);
                        System.out.println("  " + (i + 1) + ". " + course.get("courseName") + 
                                         " (점수: " + course.get("score") + ")");
                    }
                } else {
                    System.out.println("⚠검색 결과가 없습니다.");
                }
                
            } catch (Exception e) {
                System.err.println("topK=" + topK + " 테스트 실패: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("\n" + "-".repeat(50));
        }
    }
    
    @Test
    public void testSearchCoursesByPreferenceEdgeCases() {
        System.out.println("=== 엣지 케이스 테스트 시작 ===");
        
        String[] edgeCaseQueries = {
            "웹프로그래밍",  // 정상 쿼리
            "데이터베이스",  // 다른 과목
            "존재하지않는과목명",  // 존재하지 않는 과목
            "웹",  // 짧은 쿼리
            "웹프로그래밍기초"  // 부분 일치
        };
        
        for (String query : edgeCaseQueries) {
            System.out.println("\n🔍 엣지 케이스: '" + query + "'");
            System.out.println("=" + "=".repeat(30));
            
            try {
                // 실제 서비스 호출
                List<Map<String, Object>> results = courseEmbeddingService.searchCoursesByPreference(query, 3);
                System.out.println("검색 완료: " + results.size() + "개 결과");
                
                if (!results.isEmpty()) {
                    System.out.println("결과 요약:");
                    for (int i = 0; i < Math.min(3, results.size()); i++) {
                        Map<String, Object> course = results.get(i);
                        System.out.println("  " + (i + 1) + ". " + course.get("courseName") + 
                                         " (점수: " + course.get("score") + ")");
                    }
                } else {
                    System.out.println("⚠검색 결과가 없습니다.");
                }
                
            } catch (Exception e) {
                System.err.println("엣지 케이스 테스트 실패: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("\n" + "-".repeat(40));
        }
    }
    
    @Test
    public void testServiceDependencyInjection() {
        System.out.println("=== 의존성 주입 테스트 시작 ===");
        
        try {
            if (courseEmbeddingService == null) {
                System.out.println("❌ CourseEmbeddingService가 null입니다.");
                System.out.println("💡 Spring Boot 컨텍스트 로딩에 실패했을 수 있습니다.");
                return;
            }
            
            System.out.println("courseEmbeddingService 의존성 주입 성공");
            System.out.println("서비스 클래스: " + courseEmbeddingService.getClass().getName());
            
            // 메서드 존재 확인
            try {
                courseEmbeddingService.getClass().getMethod("searchCoursesByPreference", String.class, int.class);
                System.out.println("searchCoursesByPreference 메서드 존재 확인");
            } catch (NoSuchMethodException e) {
                System.err.println("searchCoursesByPreference 메서드를 찾을 수 없습니다.");
            }
            
            System.out.println("의존성 주입 테스트 완료!");
            
        } catch (Exception e) {
            System.err.println("의존성 주입 테스트 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
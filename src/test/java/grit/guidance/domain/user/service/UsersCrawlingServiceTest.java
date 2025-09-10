package grit.guidance.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import grit.guidance.domain.user.dto.HansungDataResponse;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UsersCrawlingServiceTest {

    @Autowired
    private UsersCrawlingService crawlingService;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("한성대 종합정보시스템 크롤링 테스트")
    void fetchHansungData_Success() throws Exception {
        // .env 파일 로드
        Dotenv dotenv = Dotenv.load();

        // .env 파일에서 값 읽어오기
        String studentId = dotenv.get("HANSUNG_ID");
        String password = dotenv.get("HANSUNG_PW");

        // .env 파일이나 변수가 없을 경우 테스트를 건너뜀
        if (studentId == null || password == null) {
            System.out.println(".env 파일에 HANSUNG_ID와 HANSUNG_PW를 설정해주세요. 테스트를 건너뜁니다.");
            return;
        }

        // 크롤링 서비스 메소드 실행
        HansungDataResponse response = crawlingService.fetchHansungData(studentId, password);

        // 결과가 null이 아닌지 기본 검증
        assertNotNull(response, "응답 객체는 null이 아니어야 합니다.");
        assertNotNull(response.userInfo(), "사용자 정보는 null이 아니어야 합니다.");
        assertNotNull(response.grades(), "성적 정보는 null이 아니어야 합니다.");

        // 콘솔에 결과 출력 (가장 중요한 부분)
        System.out.println("--- 크롤링 결과 ---");
        try {
            // DTO 객체를 예쁜 JSON 형식의 문자열로 변환하여 출력
            String responseAsJson = objectMapper.writeValueAsString(response);
            System.out.println(responseAsJson);
        } catch (JsonProcessingException e) {
            // 변환 실패 시 그냥 객체 toString() 결과 출력
            System.out.println(response.toString());
        }
        System.out.println("--------------------");

        // 추가 검증
        assertNotNull(response.userInfo().name(), "사용자 이름이 파싱되어야 합니다.");
        assertFalse(response.grades().semesters().isEmpty(), "최소 1개 이상의 학기 정보가 있어야 합니다.");
    }
}
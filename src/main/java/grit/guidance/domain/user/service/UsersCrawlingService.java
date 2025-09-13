package grit.guidance.domain.user.service;

import grit.guidance.domain.user.dto.*; // 위에서 만든 DTO들을 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersCrawlingService {

    private final RestTemplate restTemplate;

    private static final String HANSUNG_INFO_URL = "https://info.hansung.ac.kr";

    public HansungDataResponse fetchHansungData(String studentId, String password) throws Exception {

        // 1. 로그인 요청
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> loginBody = new LinkedMultiValueMap<>();
        loginBody.add("id", studentId);
        loginBody.add("passwd", password);

        HttpEntity<MultiValueMap<String, String>> loginRequest = new HttpEntity<>(loginBody, loginHeaders);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                HANSUNG_INFO_URL + "/servlet/s_gong.gong_login_ssl",
                loginRequest,
                String.class
        );

        // 로그인 성공 여부 확인 (ssotoken 쿠키 확인)
        List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.stream().noneMatch(c -> c.contains("ssotoken"))) {
            throw new IllegalArgumentException("로그인에 실패했습니다. 학번 또는 비밀번호를 확인해주세요.");
        }
        String sessionCookie = String.join("; ", cookies);

        // 2. 메인 페이지 GET (사용자 정보 파싱용)
        HttpHeaders mainPageHeaders = new HttpHeaders();
        mainPageHeaders.add(HttpHeaders.COOKIE, sessionCookie);

        ResponseEntity<byte[]> mainPageResponse = restTemplate.exchange(
                HANSUNG_INFO_URL + "/jsp_21/index.jsp",
                HttpMethod.GET,
                new HttpEntity<>(mainPageHeaders),
                byte[].class
        );
        // euc-kr 인코딩 처리
        String mainPageHtml = new String(mainPageResponse.getBody(), Charset.forName("euc-kr"));
        UserInfoResponse userInfo = parseUserInfoHtml(mainPageHtml);


        // 3. 성적 페이지 GET
        HttpHeaders gradePageHeaders = new HttpHeaders();
        gradePageHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        gradePageHeaders.add(HttpHeaders.REFERER, HANSUNG_INFO_URL + "/index.jsp");

        ResponseEntity<byte[]> gradePageResponse = restTemplate.exchange(
                HANSUNG_INFO_URL + "/jsp_21/student/grade/total_grade.jsp",
                HttpMethod.GET,
                new HttpEntity<>(gradePageHeaders),
                byte[].class
        );
        String gradePageHtml = new String(gradePageResponse.getBody(), Charset.forName("euc-kr"));
        TotalGradeResponse grades = parseGradeHtml(gradePageHtml);

        // 4. 결과 통합하여 반환
        HansungDataResponse result = new HansungDataResponse(userInfo, grades);
        
        // 5. 크롤링 결과 JSON 로그 출력
        log.info("=== 크롤링 결과 JSON ===");
        log.info("사용자 정보: {}", userInfo);
        log.info("성적 정보: {}", grades);
        log.info("전체 응답: {}", result);
        log.info("========================");
        
        return result;
    }

    // --- 파싱 로직 (Python의 parser.py를 Jsoup으로 구현) ---

    private UserInfoResponse parseUserInfoHtml(String html) {
        Document doc = Jsoup.parse(html);
        Element userPanel = doc.selectFirst("div.user-panel");
        if (userPanel != null) {
            Element linkTag = userPanel.selectFirst("a.d-block");
            if (linkTag != null) {
                // <br> 태그를 개행문자로 바꿔서 텍스트 추출 후 분리
                linkTag.select("br").after("\\n");
                List<String> allTexts = List.of(linkTag.text().split("\\\\n"))
                        .stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

                if (!allTexts.isEmpty()) {
                    String name = allTexts.get(allTexts.size() - 1);
                    List<String> tracks = allTexts.subList(0, allTexts.size() - 1);
                    return new UserInfoResponse(name, tracks);
                }
            }
        }
        return new UserInfoResponse(null, new ArrayList<>()); // 파싱 실패 시 기본값 반환
    }

    private TotalGradeResponse parseGradeHtml(String html) throws Exception {
        Document doc = Jsoup.parse(html);

        // (Python 코드의 로직과 1:1로 대응됩니다)
        // 전체 학점 요약 파싱
        Map<String, String> creditSummary = doc.select("#div_total .div_total_subdiv").stream()
                .map(div -> div.selectFirst("dl"))
                .filter(dl -> dl != null && dl.selectFirst("dt") != null && dl.selectFirst("dd") != null)
                .collect(Collectors.toMap(
                        dl -> dl.selectFirst("dt").text().strip(),
                        dl -> dl.selectFirst("dd").text().strip()
                ));

        // 학기별 카드 파싱
        List<SemesterGradeResponse> semesters = new ArrayList<>();
        Elements semesterCards = doc.select("div.card.divSbox");
        for (Element card : semesterCards) {
            Element headingElement = card.selectFirst(".objHeading_h3");
            if (headingElement == null) {
                continue; // 헤딩이 없는 카드는 건너뜀
            }
            String semesterName = headingElement.text().strip();

            // 학기 요약
            Map<String, String> semesterSummary = card.select(".div_total.isu .div_sub_subdiv").stream()
                    .filter(item -> item.selectFirst(".card-header") != null && item.selectFirst(".card-body") != null)
                    .collect(Collectors.toMap(
                            item -> item.selectFirst(".card-header").text().strip(),
                            item -> item.selectFirst(".card-body").text().strip()
                    ));

            // 교과목 테이블
            List<CourseGradeResponse> courses = new ArrayList<>();
            Elements rows = card.select("table.table_1 tbody tr");
            for (Element tr : rows) {
                Elements tds = tr.select("td");
                if (tds.size() >= 5) {
                    if (tds.size() >= 6) { // 6칸 테이블
                        courses.add(new CourseGradeResponse(tds.get(0).text().strip(), tds.get(1).text().strip(), tds.get(2).text().strip(), tds.get(3).text().strip(), tds.get(4).text().strip()));
                    } else { // 5칸 테이블
                        courses.add(new CourseGradeResponse(tds.get(0).text().strip(), tds.get(1).text().strip(), "", tds.get(2).text().strip(), tds.get(3).text().strip()));
                    }
                }
            }
            semesters.add(new SemesterGradeResponse(semesterName, semesterSummary, courses));
        }

        return new TotalGradeResponse(creditSummary, semesters);
    }
}
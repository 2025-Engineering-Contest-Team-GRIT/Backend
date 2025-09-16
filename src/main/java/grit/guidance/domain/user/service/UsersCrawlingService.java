package grit.guidance.domain.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    private static final String HANSUNG_INFO_URL = "https://info.hansung.ac.kr";



    /**
     * 한성대학교 데이터 크롤링 (기본 메서드)
     */
    public HansungDataResponse fetchHansungData(String studentId, String password) throws Exception {
        log.info("=== 크롤링 시작 ===");
        log.info("학번: {}", studentId);

        // 1. 로그인 요청
        log.info("1. 로그인 요청 시작");
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
        log.info("로그인 응답 상태: {}", loginResponse.getStatusCode());

        // 로그인 성공 여부 확인 (ssotoken 쿠키 확인)
        List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        log.info("로그인 쿠키: {}", cookies);
        if (cookies == null || cookies.stream().noneMatch(c -> c.contains("ssotoken"))) {
            log.error("로그인 실패 - ssotoken 쿠키 없음");
            throw new IllegalArgumentException("로그인에 실패했습니다. 학번 또는 비밀번호를 확인해주세요.");
        }
        String sessionCookie = String.join("; ", cookies);
        log.info("2. 로그인 성공, 세션 쿠키 획득");

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

        // ⭐⭐ 새로 추가된 파싱 로직 호출 ⭐⭐
        MajorRequiredCreditsResponse majorCredits = parseMajorRequiredCredits(Jsoup.parse(gradePageHtml));

        // 4. 결과 통합하여 반환
        HansungDataResponse result = new HansungDataResponse(userInfo, grades, majorCredits); // DTO에 majorCredits 필드 추가 필요

        // 5. 크롤링 결과 JSON 로그 출력
        log.info("=== 크롤링 결과 JSON ===");
        log.info("사용자 정보: {}", userInfo);
        log.info("성적 정보: {}", grades);
        log.info("전공 이수 학점: {}", majorCredits); // 로그에 추가
        // 4. (신규) 현재 수강 과목(시간표) 크롤링
        List<String> enrolledCourseNames = crawlEnrolledCourses(studentId, sessionCookie);
        
        // 5. 상세 시간표 데이터 크롤링
        List<TimetableDetailDto> timetableEvents = crawlTimetableData(studentId, sessionCookie);
        String timetableJson = objectMapper.writeValueAsString(timetableEvents);

        // 6. 결과 통합하여 반환
        HansungDataResponse result = new HansungDataResponse(userInfo, grades, enrolledCourseNames, timetableJson);
        
        // 7. 크롤링 결과 JSON 로그 출력
        log.info("=== 크롤링 결과 JSON ===");
        log.info("사용자 정보: {}", userInfo);
        log.info("성적 정보: {}", grades);
        log.info("수강 과목: {}", enrolledCourseNames);
        log.info("시간표 JSON: {}", timetableJson);
        log.info("전체 응답: {}", result);
        log.info("========================");

        return result;
    }


    // --- 파싱 로직 (Python의 parser.py를 Jsoup으로 구현) ---

    private MajorRequiredCreditsResponse parseMajorRequiredCredits(Document doc) {
        // 테이블 내의 특정 <td> 태그의 id를 이용해 값 추출
        String majorBasic1 = getCreditValueById(doc, "my_jungi1");
        String majorRequired1 = getCreditValueById(doc, "my_junji1");
        String majorSubtotal1 = getCreditValueById(doc, "my_junhap1");

        String majorBasic2 = getCreditValueById(doc, "my_jungi2");
        String majorRequired2 = getCreditValueById(doc, "my_junji2");
        String majorSubtotal2 = getCreditValueById(doc, "my_junhap2");

        String totalCompleted = getCreditValueById(doc, "my_juntotal");
        String totalRequired = getCreditRequiredValueById(doc, "standard_juntotal");

        // DTO에 데이터를 담아 반환
        return MajorRequiredCreditsResponse.builder()
                .track1(MajorCreditDetail.builder()
                        .majorBasic(majorBasic1)
                        .majorRequired(majorRequired1)
                        .majorSubtotal(majorSubtotal1)
                        .build())
                .track2(MajorCreditDetail.builder()
                        .majorBasic(majorBasic2)
                        .majorRequired(majorRequired2)
                        .majorSubtotal(majorSubtotal2)
                        .build())
                .total(MajorCreditTotal.builder()
                        .completed(totalCompleted)
                        .required(totalRequired)
                        .build())
                .build();
    }

    private String getCreditValueById(Document doc, String id) {
        Element element = doc.selectFirst("#" + id);
        return (element != null) ? element.text().trim() : null;
    }

    // 총합계 필드는 괄호 안의 필수 학점도 함께 추출
    private String getCreditRequiredValueById(Document doc, String id) {
        Element element = doc.selectFirst("#" + id);
        return (element != null) ? element.text().trim() : null;
    }

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
                if (tds.size() >= 6) { // 6칸 테이블 (구분, 교과명, 교과코드, 학점, 성적, 현재트랙)
                    String classification = tds.get(0).text().strip();
                    String name = tds.get(1).text().strip();
                    String code = tds.get(2).text().strip();
                    String credits = tds.get(3).text().strip();
                    String grade = tds.get(4).text().strip();
                    String rawTrackStatus = tds.get(5).text().strip();
                    String trackStatus = parseTrackStatus(rawTrackStatus);
                    
                    courses.add(new CourseGradeResponse(classification, name, code, credits, grade, trackStatus));
                } else if (tds.size() >= 5) { // 5칸 테이블 (구분, 교과명, 학점, 성적, 현재트랙)
                    String classification = tds.get(0).text().strip();
                    String name = tds.get(1).text().strip();
                    String code = ""; // 교과코드가 없는 경우
                    String credits = tds.get(2).text().strip();
                    String grade = tds.get(3).text().strip();
                    String rawTrackStatus = tds.get(4).text().strip();
                    String trackStatus = parseTrackStatus(rawTrackStatus);
                    
                    courses.add(new CourseGradeResponse(classification, name, code, credits, grade, trackStatus));
                }
            }
            semesters.add(new SemesterGradeResponse(semesterName, semesterSummary, courses));
        }

        return new TotalGradeResponse(creditSummary, semesters);
    }

    /**
     * 트랙 상태 문자열을 파싱하여 간단한 트랙명만 반환
     * "현재 : 제1트랙 제2트랙변경시(전선)" -> "제1트랙"
     * "현재 : 제2트랙" -> "제2트랙"
     * "선필교(사회과학 분야)" -> ""
     */
    private String parseTrackStatus(String trackStatus) {
        if (trackStatus == null || trackStatus.trim().isEmpty()) {
            return "";
        }

        // "현재 : 제1트랙" 또는 "현재 : 제2트랙" 형태에서 트랙 번호 추출
        if (trackStatus.startsWith("현재 : 제1트랙")) {
            return "제1트랙";
        } else if (trackStatus.startsWith("현재 : 제2트랙")) {
            return "제2트랙";
        } else if (trackStatus.contains("선필교") || trackStatus.contains("교필") || trackStatus.contains("일선")) {
            // 교양 과목들은 빈 문자열 반환
            return "";
        } else {
            // 알 수 없는 경우 빈 문자열 반환
            return "";
        }
    }

    /**
     * 현재 수강 중인 과목들을 시간표에서 크롤링하여 과목명 리스트를 반환
     */
    private List<String> crawlEnrolledCourses(String studentId, String sessionCookie) throws Exception {
        String timetableDataUrl = HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_sigan_main_data.jsp";
        
        // 시간표 데이터 요청 헤더 설정
        HttpHeaders timetableHeaders = new HttpHeaders();
        timetableHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        timetableHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        timetableHeaders.add(HttpHeaders.REFERER, HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_h_siganpyo.jsp");
        
        // 요청 본문 설정
        MultiValueMap<String, String> timetableBody = new LinkedMultiValueMap<>();
        timetableBody.add("as_hakbun", studentId);
        
        HttpEntity<MultiValueMap<String, String>> timetableRequest = new HttpEntity<>(timetableBody, timetableHeaders);

        // POST 요청 실행
        ResponseEntity<String> response = restTemplate.postForEntity(timetableDataUrl, timetableRequest, String.class);
        String responseBody = response.getBody();

        // 디버깅 로그
        log.info("시간표 데이터 요청 결과: {}", responseBody);

        // JSON 응답 파싱
        List<TimetableEventDto> events = objectMapper.readValue(responseBody, new TypeReference<>() {});

        return events.stream()
                .map(TimetableEventDto::title)
                .filter(title -> title != null && !title.trim().isEmpty())
                .map(this::extractCourseName) // 과목명만 추출
                .distinct()
                .toList();
    }

    /**
     * 시간표 데이터를 크롤링하여 상세한 TimetableDetailDto 리스트를 반환
     */
    private List<TimetableDetailDto> crawlTimetableData(String studentId, String sessionCookie) throws Exception {
        String timetableDataUrl = HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_sigan_main_data.jsp";
        
        // 시간표 데이터 요청 헤더 설정
        HttpHeaders timetableHeaders = new HttpHeaders();
        timetableHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        timetableHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        timetableHeaders.add(HttpHeaders.REFERER, HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_h_siganpyo.jsp");
        
        // 요청 본문 설정
        MultiValueMap<String, String> timetableBody = new LinkedMultiValueMap<>();
        timetableBody.add("as_hakbun", studentId);
        
        HttpEntity<MultiValueMap<String, String>> timetableRequest = new HttpEntity<>(timetableBody, timetableHeaders);

        // POST 요청 실행
        ResponseEntity<String> response = restTemplate.postForEntity(timetableDataUrl, timetableRequest, String.class);
        String responseBody = response.getBody();

        // 디버깅 로그
        log.info("시간표 데이터 요청 결과: {}", responseBody);

        // JSON 응답 파싱
        List<TimetableEventDto> rawEvents = objectMapper.readValue(responseBody, new TypeReference<>() {});
        
        // title을 파싱하여 과목명, 교수명, 강의실로 분리 (시간 정보는 빈 문자열로)
        List<TimetableDetailDto> parsedEvents = rawEvents.stream()
                .map(this::parseTimetableEventFromJson)
                .toList();
        
        return parsedEvents;
    }


    /**
     * JSON 응답에서 시간표 이벤트를 파싱하여 TimetableDetailDto로 변환
     */
    private TimetableDetailDto parseTimetableEventFromJson(TimetableEventDto event) {
        if (event.title() == null || event.title().trim().isEmpty()) {
            return new TimetableDetailDto(
                "",
                "",
                "",
                "",
                ""
            );
        }
        
        // 개행문자(\n)로 분리
        String[] parts = event.title().split("\n");
        
        String courseName = "";
        String professorName = "";
        String classroom = "";
        
        if (parts.length >= 1) {
            // 과목명에서 괄호 부분 제거 (예: "설계패턴(C)" -> "설계패턴")
            courseName = parts[0].trim().replaceAll("\\([^)]*\\)", "").trim();
        }
        
        if (parts.length >= 2) {
            professorName = parts[1].trim();
        }
        
        if (parts.length >= 3) {
            classroom = parts[2].trim();
        }
        
        return new TimetableDetailDto(
            courseName,
            professorName,
            classroom,
            event.start() != null ? event.start() : "",
            event.end() != null ? event.end() : ""
        );
    }

    private String extractCourseName(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "";
        }
        
        // 개행문자(\n)로 분리하여 첫 번째 부분(과목명)만 추출
        String[] parts = title.split("\n");
        if (parts.length > 0) {
            String courseName = parts[0].trim();
            return courseName.replaceAll("\\([^)]*\\)", "").trim();
        }
        
        return title.trim();
    }
}
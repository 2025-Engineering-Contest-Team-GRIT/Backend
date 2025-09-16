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
        List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies == null || cookies.stream().noneMatch(c -> c.contains("ssotoken"))) {
            throw new IllegalArgumentException("로그인에 실패했습니다. 학번 또는 비밀번호를 확인해주세요.");
        }
        String sessionCookie = String.join("; ", cookies);

        // 2. 메인 페이지 GET (사용자 정보 파싱)
        HttpHeaders mainPageHeaders = new HttpHeaders();
        mainPageHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        ResponseEntity<byte[]> mainPageResponse = restTemplate.exchange(
                HANSUNG_INFO_URL + "/jsp_21/index.jsp",
                HttpMethod.GET,
                new HttpEntity<>(mainPageHeaders),
                byte[].class
        );
        String mainPageHtml = new String(mainPageResponse.getBody(), Charset.forName("euc-kr"));
        UserInfoResponse userInfo = parseUserInfoHtml(mainPageHtml);

        // 3. 성적 페이지 GET 및 파싱
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
        MajorRequiredCreditsResponse majorCredits = parseMajorRequiredCredits(Jsoup.parse(gradePageHtml));

        // 4. 시간표 데이터 크롤링
        List<String> enrolledCourseNames = crawlEnrolledCourses(studentId, sessionCookie);
        List<TimetableDetailDto> timetableEvents = crawlTimetableData(studentId, sessionCookie);
        String timetableJson = objectMapper.writeValueAsString(timetableEvents);

        // 5. 모든 결과를 통합하여 반환
        HansungDataResponse result = new HansungDataResponse(
                userInfo,
                grades,
                majorCredits,
                enrolledCourseNames,
                timetableJson
        );

        log.info("=== 크롤링 결과 JSON ===");
        log.info("사용자 정보: {}", userInfo);
        log.info("성적 정보: {}", grades);
        log.info("전공 이수 학점: {}", majorCredits);
        log.info("수강 과목: {}", enrolledCourseNames);
        log.info("시간표 JSON: {}", timetableJson);
        log.info("전체 응답: {}", result);
        log.info("========================");

        return result;
    }


    // --- 파싱 로직 ---

    private MajorRequiredCreditsResponse parseMajorRequiredCredits(Document doc) {
        // ... (기존 로직과 동일) ...
        String majorBasic1 = getCreditValueById(doc, "my_jungi1");
        String majorRequired1 = getCreditValueById(doc, "my_junji1");
        String majorSubtotal1 = getCreditValueById(doc, "my_junhap1");

        String majorBasic2 = getCreditValueById(doc, "my_jungi2");
        String majorRequired2 = getCreditValueById(doc, "my_junji2");
        String majorSubtotal2 = getCreditValueById(doc, "my_junhap2");

        String totalCompleted = getCreditValueById(doc, "my_juntotal");
        String totalRequired = getCreditRequiredValueById(doc, "standard_juntotal");

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
        return new UserInfoResponse(null, new ArrayList<>());
    }

    private TotalGradeResponse parseGradeHtml(String html) throws Exception {
        Document doc = Jsoup.parse(html);

        Map<String, String> creditSummary = doc.select("#div_total .div_total_subdiv").stream()
                .map(div -> div.selectFirst("dl"))
                .filter(dl -> dl != null && dl.selectFirst("dt") != null && dl.selectFirst("dd") != null)
                .collect(Collectors.toMap(
                        dl -> dl.selectFirst("dt").text().strip(),
                        dl -> dl.selectFirst("dd").text().strip()
                ));

        List<SemesterGradeResponse> semesters = new ArrayList<>();
        Elements semesterCards = doc.select("div.card.divSbox");
        for (Element card : semesterCards) {
            Element headingElement = card.selectFirst(".objHeading_h3");
            if (headingElement == null) {
                continue;
            }
            String semesterName = headingElement.text().strip();

            Map<String, String> semesterSummary = card.select(".div_total.isu .div_sub_subdiv").stream()
                    .filter(item -> item.selectFirst(".card-header") != null && item.selectFirst(".card-body") != null)
                    .collect(Collectors.toMap(
                            item -> item.selectFirst(".card-header").text().strip(),
                            item -> item.selectFirst(".card-body").text().strip()
                    ));

            List<CourseGradeResponse> courses = new ArrayList<>();
            Elements rows = card.select("table.table_1 tbody tr");
            for (Element tr : rows) {
                Elements tds = tr.select("td");
                if (tds.size() >= 6) {
                    String classification = tds.get(0).text().strip();
                    String name = tds.get(1).text().strip();
                    String code = tds.get(2).text().strip();
                    String credits = tds.get(3).text().strip();
                    String grade = tds.get(4).text().strip();
                    String rawTrackStatus = tds.get(5).text().strip();
                    String trackStatus = parseTrackStatus(rawTrackStatus);

                    courses.add(new CourseGradeResponse(classification, name, code, credits, grade, trackStatus));
                } else if (tds.size() >= 5) {
                    String classification = tds.get(0).text().strip();
                    String name = tds.get(1).text().strip();
                    String code = "";
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
     */
    private String parseTrackStatus(String trackStatus) {
        if (trackStatus == null || trackStatus.trim().isEmpty()) {
            return "";
        }

        if (trackStatus.startsWith("현재 : 제1트랙")) {
            return "제1트랙";
        } else if (trackStatus.startsWith("현재 : 제2트랙")) {
            return "제2트랙";
        } else if (trackStatus.contains("선필교") || trackStatus.contains("교필") || trackStatus.contains("일선")) {
            return "";
        } else {
            return "";
        }
    }

    /**
     * 현재 수강 중인 과목들을 시간표에서 크롤링하여 과목명 리스트를 반환
     */
    private List<String> crawlEnrolledCourses(String studentId, String sessionCookie) throws Exception {
        String timetableDataUrl = HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_sigan_main_data.jsp";

        HttpHeaders timetableHeaders = new HttpHeaders();
        timetableHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        timetableHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        timetableHeaders.add(HttpHeaders.REFERER, HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_h_siganpyo.jsp");

        MultiValueMap<String, String> timetableBody = new LinkedMultiValueMap<>();
        timetableBody.add("as_hakbun", studentId);

        HttpEntity<MultiValueMap<String, String>> timetableRequest = new HttpEntity<>(timetableBody, timetableHeaders);

        ResponseEntity<String> response = restTemplate.postForEntity(timetableDataUrl, timetableRequest, String.class);
        String responseBody = response.getBody();

        log.info("시간표 데이터 요청 결과: {}", responseBody);

        List<TimetableEventDto> events = objectMapper.readValue(responseBody, new TypeReference<>() {});

        return events.stream()
                .map(TimetableEventDto::title)
                .filter(title -> title != null && !title.trim().isEmpty())
                .map(this::extractCourseName)
                .distinct()
                .toList();
    }

    /**
     * 시간표 데이터를 크롤링하여 상세한 TimetableDetailDto 리스트를 반환
     */
    private List<TimetableDetailDto> crawlTimetableData(String studentId, String sessionCookie) throws Exception {
        String timetableDataUrl = HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_sigan_main_data.jsp";

        HttpHeaders timetableHeaders = new HttpHeaders();
        timetableHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        timetableHeaders.add(HttpHeaders.COOKIE, sessionCookie);
        timetableHeaders.add(HttpHeaders.REFERER, HANSUNG_INFO_URL + "/jsp_21/student/kyomu/dae_h_siganpyo.jsp");

        MultiValueMap<String, String> timetableBody = new LinkedMultiValueMap<>();
        timetableBody.add("as_hakbun", studentId);

        HttpEntity<MultiValueMap<String, String>> timetableRequest = new HttpEntity<>(timetableBody, timetableHeaders);

        ResponseEntity<String> response = restTemplate.postForEntity(timetableDataUrl, timetableRequest, String.class);
        String responseBody = response.getBody();

        log.info("시간표 데이터 요청 결과: {}", responseBody);

        List<TimetableEventDto> rawEvents = objectMapper.readValue(responseBody, new TypeReference<>() {});

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

        String[] parts = event.title().split("\n");

        String courseName = "";
        String professorName = "";
        String classroom = "";

        if (parts.length >= 1) {
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

        String[] parts = title.split("\n");
        if (parts.length > 0) {
            String courseName = parts[0].trim();
            return courseName.replaceAll("\\([^)]*\\)", "").trim();
        }

        return title.trim();
    }
}
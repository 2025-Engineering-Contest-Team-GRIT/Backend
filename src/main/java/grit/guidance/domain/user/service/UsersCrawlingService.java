package grit.guidance.domain.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import grit.guidance.domain.course.entity.Semester;
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
import java.time.LocalDate;
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
        
        // 학년과 학기 정보 파싱
        log.info("학년과 학기 정보 파싱 시작");
        Integer grade = parseGradeFromHtml(gradePageHtml);
        Semester semester = calculateCurrentSemester();
        log.info("파싱된 학년: {}, 학기: {}", grade, semester);

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
                timetableJson,
                grade,
                semester
        );

        // 6. 한성대 사이트 로그아웃 (세션 정리)
        try {
            logoutFromHansung(sessionCookie);
            log.info("한성대 사이트 로그아웃 완료");
        } catch (Exception e) {
            log.warn("한성대 사이트 로그아웃 실패: {}", e.getMessage());
        }

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


    /**
     * 한성대 사이트에서 로그아웃 처리
     */
    private void logoutFromHansung(String sessionCookie) {
        try {
            HttpHeaders logoutHeaders = new HttpHeaders();
            logoutHeaders.add(HttpHeaders.COOKIE, sessionCookie);
            logoutHeaders.add(HttpHeaders.REFERER, HANSUNG_INFO_URL + "/index.jsp");
            
            // 로그아웃 요청
            restTemplate.exchange(
                HANSUNG_INFO_URL + "/servlet/s_gong.gong_logout",
                HttpMethod.GET,
                new HttpEntity<>(logoutHeaders),
                String.class
            );
            
            log.info("한성대 사이트 로그아웃 요청 완료");
        } catch (Exception e) {
            log.warn("한성대 사이트 로그아웃 중 오류: {}", e.getMessage());
        }
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

    /**
     * 성적 페이지에서 학년 정보를 파싱
     */
    private Integer parseGradeFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        // "변정원 (2271187) 컴퓨터공학부 3 학년 복학" 형태에서 학년 추출
        Element strongElement = doc.selectFirst("strong.objHeading_h3");
        if (strongElement != null) {
            String text = strongElement.text();
            log.info("학년 파싱 시도 - 찾은 텍스트: {}", text);
            // 정규식으로 "숫자 학년" 패턴 찾기
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s*학년");
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                Integer grade = Integer.parseInt(matcher.group(1));
                log.info("파싱된 학년: {}", grade);
                return grade;
            } else {
                log.warn("학년 패턴을 찾을 수 없음: {}", text);
            }
        } else {
            log.warn("strong.objHeading_h3 요소를 찾을 수 없음");
        }
        return null;
    }

    /**
     * 현재 날짜를 기준으로 학기 계산
     * 3월 2일부터 9월 1일 전까지는 1학기, 9월 1일부터 3월 2일 전까지는 2학기
     */
    private Semester calculateCurrentSemester() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        
        log.info("현재 날짜: {}월 {}일", month, day);
        
        // 3월 2일부터 9월 1일 전까지는 1학기
        if ((month == 3 && day >= 2) || (month > 3 && month < 9) || (month == 9 && day < 1)) {
            log.info("계산된 학기: 1학기");
            return Semester.FIRST;
        } else {
            log.info("계산된 학기: 2학기");
            return Semester.SECOND;
        }
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

        // 날짜에서 요일 계산 및 시간 형식 변경
        String day = extractDayFromDateTime(event.start());
        String startTime = extractTimeFromDateTime(event.start());
        String endTime = extractTimeFromDateTime(event.end());

        return new TimetableDetailDto(
                courseName,
                professorName,
                classroom,
                day,
                startTime,
                endTime
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
    
    /**
     * 날짜시간 문자열에서 요일을 추출합니다.
     * 예: "2025-09-18T10:30:00" -> "목"
     */
    private String extractDayFromDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return "";
        }
        
        try {
            // "2025-09-18T10:30:00" 형태에서 날짜 부분만 추출
            String datePart = dateTime.split("T")[0];
            LocalDate date = LocalDate.parse(datePart);
            
            // 요일을 한글로 변환
            return switch (date.getDayOfWeek()) {
                case MONDAY -> "월";
                case TUESDAY -> "화";
                case WEDNESDAY -> "수";
                case THURSDAY -> "목";
                case FRIDAY -> "금";
                case SATURDAY -> "토";
                case SUNDAY -> "일";
            };
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateTime);
            return "";
        }
    }
    
    /**
     * 날짜시간 문자열에서 시간만 추출합니다.
     * 예: "2025-09-18T10:30:00" -> "10:30:00"
     */
    private String extractTimeFromDateTime(String dateTime) {
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return "";
        }
        
        try {
            // "2025-09-18T10:30:00" 형태에서 시간 부분만 추출
            if (dateTime.contains("T")) {
                return dateTime.split("T")[1];
            }
            return dateTime;
        } catch (Exception e) {
            log.warn("시간 파싱 실패: {}", dateTime);
            return "";
        }
    }
}
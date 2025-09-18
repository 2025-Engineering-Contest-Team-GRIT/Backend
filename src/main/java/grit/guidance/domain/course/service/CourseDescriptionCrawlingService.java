package grit.guidance.domain.course.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseDescriptionCrawlingService {

    private final CourseRepository courseRepository;
    
    // 4개 트랙 URL
    private static final String[] TRACK_URLS = {
        "https://hansung.ac.kr/CSE/10753/subview.do", // 모바일소프트웨어
        "https://hansung.ac.kr/CSE/10754/subview.do", // 웹공학
        "https://hansung.ac.kr/CSE/10755/subview.do", // 빅데이터
        "https://hansung.ac.kr/CSE/10756/subview.do"  // 디지털콘텐츠 및 가상현실
    };

    //데이터베이스의 Course 엔티티에서 설명이 없는 과목들의 설명을 크롤링하여 업데이트
    @Transactional
    public void updateCourseDescriptionsFromDatabase() {
        try {
            log.info("데이터베이스에서 설명이 없는 과목들을 크롤링 시작");
            
            // 설명이 없는 과목들 조회
            List<Course> coursesWithoutDescription = courseRepository.findAll().stream()
                    .filter(course -> course.getDescription() == null || course.getDescription().trim().isEmpty())
                    .toList();
            
            log.info("설명이 없는 과목 수: {}", coursesWithoutDescription.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Course course : coursesWithoutDescription) {
                try {
                    String description = crawlCourseDescriptionFromAllTracks(course.getCourseCode(), course.getCourseName());
                    
                    if (description != null && !description.trim().isEmpty()) {
                        // Course 엔티티 업데이트
                        course.updateDescription(description);
                        courseRepository.save(course);
                        successCount++;
                        log.info("과목 설명 업데이트 성공: {} - {}", course.getCourseCode(), course.getCourseName());
                    } else {
                        failCount++;
                        log.warn("과목 설명을 찾을 수 없음: {} - {}", course.getCourseCode(), course.getCourseName());
                    }
                    
                    // 서버 부하 방지를 위한 대기
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("과목 설명 크롤링 실패: {} - {}", course.getCourseCode(), course.getCourseName(), e);
                }
            }
            
            log.info("크롤링 완료 - 성공: {}, 실패: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("데이터베이스 과목 설명 업데이트 실패", e);
        }
    }

    //4개 트랙에서 과목 설명을 찾아서 크롤링 (첫 번째로 찾은 설명을 반환)
    public String crawlCourseDescriptionFromAllTracks(String courseId, String courseName) {
        for (String trackUrl : TRACK_URLS) {
            try {
                log.info("트랙에서 과목 검색: {} - {} (URL: {})", courseId, courseName, trackUrl);
                
                Document doc = Jsoup.connect(trackUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(15000)
                        .get();
                
                // 과목 목록에서 해당 과목 찾기
                Elements courseElements = doc.select("table tr");
                
                for (Element row : courseElements) {
                    Elements cells = row.select("td");
                    if (cells.size() >= 5) {
                        String cellCourseId = cells.get(3).text().trim();
                        String cellCourseName = cells.get(4).text().trim();
                        
                        // 과목 코드나 과목명이 일치하는지 확인
                        boolean isMatch = courseId.equals(cellCourseId) || 
                                        courseName.equals(cellCourseName) ||
                                        cellCourseName.contains(courseName) ||
                                        courseName.contains(cellCourseName);
                        
                        if (isMatch) {
                            log.info("과목 매칭 성공: {} - {} (트랙: {})", courseId, courseName, trackUrl);
                            
                            // 과목 링크 찾기
                            Element linkElement = cells.get(4).selectFirst("a");
                            if (linkElement != null) {
                                String detailUrl = linkElement.attr("href");
                                if (detailUrl.startsWith("/")) {
                                    detailUrl = "https://hansung.ac.kr" + detailUrl;
                                }
                                
                                log.info("과목 링크 발견: {} - {}", courseName, detailUrl);
                                
                                // 상세 페이지에서 설명 크롤링
                                String description = crawlDescriptionFromDetailPage(detailUrl);
                                if (description != null && !description.trim().isEmpty()) {
                                    log.info("과목 설명 크롤링 완료: {} - {} (트랙: {})", courseId, courseName, trackUrl);
                                    return description; // 설명을 찾으면 바로 반환하고 나머지 트랙은 검색하지 않음
                                }
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                log.warn("트랙에서 과목 검색 실패: {} (URL: {})", courseId, trackUrl, e.getMessage());
            }
        }
        
        log.warn("모든 트랙에서 과목을 찾을 수 없음: {} - {}", courseId, courseName);
        return null;
    }


    private String crawlDescriptionFromDetailPage(String detailUrl) {
        try {
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            
            // 상세 페이지에서 설명 찾기 - 여러 방법 시도
            String description = null;
            
            // 1. p.content 클래스에서 찾기
            Elements contentElements = doc.select("p.content");
            if (!contentElements.isEmpty()) {
                description = contentElements.first().text().trim();
                log.info("p.content에서 설명 발견: {}", description);
            }
            
            // 2. table 다음에 오는 p 태그에서 찾기
            if (description == null || description.isEmpty()) {
                Elements tableElements = doc.select("table");
                if (!tableElements.isEmpty()) {
                    Element table = tableElements.first();
                    Element nextP = table.nextElementSibling();
                    if (nextP != null && "p".equals(nextP.tagName())) {
                        description = nextP.text().trim();
                        log.info("table 다음 p 태그에서 설명 발견: {}", description);
                    }
                }
            }
            
            // 3. 모든 p 태그에서 긴 텍스트 찾기
            if (description == null || description.isEmpty()) {
                Elements allPElements = doc.select("p");
                for (Element p : allPElements) {
                    String text = p.text().trim();
                    if (text.length() > 50) { // 충분히 긴 설명만 선택
                        description = text;
                        log.info("긴 p 태그에서 설명 발견: {}", description);
                        break;
                    }
                }
            }
            
            if (description != null && !description.isEmpty()) {
                log.info("최종 과목 설명: {}", description);
                return description;
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("상세 페이지 크롤링 실패: {}", detailUrl, e);
            return null;
        }
    }


    private String extractDescriptionFromText(String text) {
        // 간단한 설명 추출 로직
        String[] lines = text.split("\n");
        StringBuilder description = new StringBuilder();
        
        boolean inDescription = false;
        for (String line : lines) {
            line = line.trim();
            if (line.contains("학습목표") || line.contains("강의내용") || line.contains("과목소개")) {
                inDescription = true;
                continue;
            }
            if (inDescription && !line.isEmpty() && line.length() > 10) {
                description.append(line).append(" ");
                if (description.length() > 500) break; // 너무 길면 자르기
            }
        }
        
        return description.toString().trim();
    }
}

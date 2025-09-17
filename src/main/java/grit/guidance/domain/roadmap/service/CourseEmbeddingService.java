package grit.guidance.domain.roadmap.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.roadmap.repository.QdrantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(CourseEmbeddingService.class);
    
    private final CourseRepository courseRepository;
    private final QdrantRepository qdrantRepository;

    /**
     * 모든 과목을 Qdrant에 저장
     */
    public void embedAllCourses() {
        try {
            log.info("과목 데이터 벡터화 및 Qdrant 저장 시작");

            // 1. 모든 과목 조회
            List<Course> courses = courseRepository.findAll();
            log.info("총 {}개의 과목을 조회했습니다.", courses.size());
            
            if (courses.isEmpty()) {
                log.warn("조회된 과목이 없습니다.");
                return;
            }

            // 2. Map으로 변환
            List<Map<String, Object>> documents = courses.stream()
                    .map(this::createCourseDocument)
                    .collect(Collectors.toList());

            // 3. Qdrant에 저장
            qdrantRepository.addCourseDocuments(documents);
            log.info("{}개의 과목을 Qdrant에 성공적으로 저장했습니다.", documents.size());

        } catch (Exception e) {
            log.error("과목 데이터 벡터화 및 저장 실패", e);
            throw new RuntimeException("과목 데이터 저장에 실패했습니다.", e);
        }
    }

    /**
     * 특정 과목을 Qdrant에 저장
     */
    public void embedCourse(Course course) {
        try {
            log.info("과목 벡터화 및 저장: {} - {}", course.getCourseCode(), course.getCourseName());

            Map<String, Object> document = createCourseDocument(course);
            qdrantRepository.addCourseDocument(document);

            log.info("과목 저장 완료: {} - {}", course.getCourseCode(), course.getCourseName());

        } catch (Exception e) {
            log.error("과목 저장 실패: {} - {}", course.getCourseCode(), course.getCourseName(), e);
            throw new RuntimeException("과목 저장에 실패했습니다.", e);
        }
    }

    /**
     * Course 엔티티를 Map으로 변환
     */
    private Map<String, Object> createCourseDocument(Course course) {
        // 1. 벡터화할 텍스트 생성
        String text = createEmbeddingText(course);

        // 2. 메타데이터 생성
        Map<String, Object> metadata = createCourseMetadata(course);

        // 3. Map에 ID만 추가 (text 필드는 제거)
        metadata.put("id", "course_" + course.getId());

        return metadata;
    }

    /**
     * 벡터화할 텍스트 생성
     */
    private String createEmbeddingText(Course course) {
        StringBuilder text = new StringBuilder();

        // 과목명
        text.append(course.getCourseName()).append(" ");

        // 과목 설명 (있는 경우)
        if (course.getDescription() != null && !course.getDescription().trim().isEmpty()) {
            text.append(course.getDescription()).append(" ");
        }

        // 과목 코드
        text.append(course.getCourseCode()).append(" ");

        // 학점 정보
        text.append(course.getCredits()).append("학점 ");

        // 개설 학년/학기
        text.append(course.getOpenGrade()).append("학년 ");
        text.append(course.getOpenSemester().name()).append(" ");

        return text.toString().trim();
    }

    /**
     * 과목 메타데이터 생성
     */
    private Map<String, Object> createCourseMetadata(Course course) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("courseId", course.getId());
        metadata.put("courseCode", course.getCourseCode());
        metadata.put("courseName", course.getCourseName());
        metadata.put("description", course.getDescription() != null ? course.getDescription() : "");
        metadata.put("credits", course.getCredits());
        metadata.put("openGrade", course.getOpenGrade());
        metadata.put("openSemester", course.getOpenSemester().name());
        metadata.put("type", "course");
        return metadata;
    }

    /**
     * Qdrant에서 과목 검색 (테스트용)
     */
    public List<Map<String, Object>> searchCourses(String query, int topK) {
        try {
            log.info("과목 검색: {} (상위 {}개)", query, topK);
            return qdrantRepository.searchSimilarCourses(query, topK);
        } catch (Exception e) {
            log.error("과목 검색 실패: {}", query, e);
            throw new RuntimeException("과목 검색에 실패했습니다.", e);
        }
    }

    /**
     * Qdrant 상태 확인
     */
    public boolean isQdrantHealthy() {
        return qdrantRepository.isVectorStoreHealthy();
    }

    /**
     * 저장된 과목 개수 확인
     */
    public long getStoredCourseCount() {
        return qdrantRepository.getDocumentCount();
    }
}
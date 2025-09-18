package grit.guidance.domain.roadmap.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.TrackRequirement;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
import grit.guidance.domain.roadmap.dto.CourseRecommendationRequest;
import grit.guidance.domain.roadmap.repository.QdrantRepository;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.EnrolledCourseRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import grit.guidance.domain.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(CourseEmbeddingService.class);
    
    private final CourseRepository courseRepository;
    private final TrackRequirementRepository trackRequirementRepository;
    private final QdrantRepository qdrantRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final EnrolledCourseRepository enrolledCourseRepository;
    private final UsersRepository usersRepository;
    private final LlmRoadmapService llmRoadmapService;

    /**
     * 모든 과목을 Qdrant에 저장 (트랙 정보 포함)
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

            // 2. 과목을 Map으로 변환 (트랙 정보 포함)
            List<Map<String, Object>> courseDocuments = courses.stream()
                    .map(this::createCourseDocumentWithTracks)
                    .collect(Collectors.toList());

            // 3. Qdrant에 저장
            qdrantRepository.addCourseDocuments(courseDocuments);
            log.info("{}개의 과목을 Qdrant에 성공적으로 저장했습니다.", courseDocuments.size());

        } catch (Exception e) {
            log.error("과목 데이터 벡터화 및 저장 실패", e);
            throw new RuntimeException("데이터 저장에 실패했습니다.", e);
        }
    }

    /**
     * 특정 과목을 Qdrant에 저장
     */
    public void embedCourse(Course course) {
        try {
            log.info("과목 벡터화 및 저장: {} - {}", course.getCourseCode(), course.getCourseName());

            Map<String, Object> document = createCourseDocumentWithTracks(course);
            qdrantRepository.addCourseDocument(document);

            log.info("과목 저장 완료: {} - {}", course.getCourseCode(), course.getCourseName());

        } catch (Exception e) {
            log.error("과목 저장 실패: {} - {}", course.getCourseCode(), course.getCourseName(), e);
            throw new RuntimeException("과목 저장에 실패했습니다.", e);
        }
    }

    /**
     * Course 엔티티를 Map으로 변환 (트랙 정보 포함)
     */
    private Map<String, Object> createCourseDocumentWithTracks(Course course) {
        // 1. 벡터화할 텍스트 생성 (트랙 정보 포함)
        String text = createEmbeddingTextWithTracks(course);

        // 2. 메타데이터 생성 (트랙 정보 포함)
        Map<String, Object> metadata = createCourseMetadataWithTracks(course);

        // 3. Map에 ID와 텍스트 추가
        metadata.put("id", "course_" + course.getId());
        metadata.put("text", text);

        return metadata;
    }

    /**
     * 벡터화할 텍스트 생성 (트랙 정보 포함)
     */
    private String createEmbeddingTextWithTracks(Course course) {
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

        // 트랙 정보 추가 (TrackRequirement를 통해)
        List<TrackRequirement> trackRequirements = trackRequirementRepository.findByCourseId(course.getId());
        for (TrackRequirement tr : trackRequirements) {
            text.append(tr.getTrack().getTrackName()).append(" ");
            text.append(tr.getCourseType().getDescription()).append(" ");
        }

        return text.toString().trim();
    }

    /**
     * 과목 메타데이터 생성 (트랙 정보 포함)
     */
    private Map<String, Object> createCourseMetadataWithTracks(Course course) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("courseId", course.getId());
        metadata.put("courseCode", course.getCourseCode());
        metadata.put("courseName", course.getCourseName());
        metadata.put("description", course.getDescription() != null ? course.getDescription() : "");
        metadata.put("credits", course.getCredits());
        metadata.put("openGrade", course.getOpenGrade());
        metadata.put("openSemester", course.getOpenSemester().name());
        metadata.put("type", "course");

        // 트랙 정보 추가 (TrackRequirement를 통해)
        List<TrackRequirement> trackRequirements = trackRequirementRepository.findByCourseId(course.getId());
        
        // 트랙 목록만 저장
        List<String> tracks = trackRequirements.stream()
                .map(tr -> tr.getTrack().getTrackName())
                .collect(Collectors.toList());
        metadata.put("tracks", tracks);

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
     * 사용자 요청 기반 과목 검색 (2단계: 취향 저격 과목)
     */
    public List<Map<String, Object>> searchCoursesByPreference(String userQuery, int topK) {
        try {
            log.info("CourseEmbeddingService 검색 시작: query='{}', topK={}", userQuery, topK);
            
            // Qdrant에서 유사도 검색
            List<Map<String, Object>> searchResults = qdrantRepository.searchSimilarCourses(userQuery, topK);
            
            log.info("CourseEmbeddingService 검색 완료: {}개의 과목을 찾았습니다.", searchResults.size());
            return searchResults;
            
        } catch (Exception e) {
            log.error("CourseEmbeddingService 과목 검색 실패: {}", userQuery, e);
            throw new RuntimeException("과목 검색에 실패했습니다.", e);
        }
    }

    /**
     * 1단계: 필수 과목 목록 확보 (규칙 기반 필터링)
     * 학생의 트랙에서 전공필수(MANDATORY)와 전공기초(FOUNDATION) 과목 중 아직 이수하지 않은 과목들을 반환
     */
    public List<Map<String, Object>> getMandatoryCourses(List<Long> trackIds, String studentId) {
        try {
            log.info("1단계: 필수 과목 목록 확보 시작 - trackIds={}, studentId={}", trackIds, studentId);

            // 1. 학번으로 사용자 찾기
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("학번 " + studentId + "에 해당하는 사용자를 찾을 수 없습니다."));

            // 2. 사용자의 트랙에서 전공필수/전공기초 과목 조회
            List<TrackRequirement> trackRequirements = trackRequirementRepository.findByTrackIdsAndCourseType(trackIds);
            
            // 3. 이미 이수한 과목과 수강중인 과목 ID 목록 조회
            List<Long> completedCourseIds = completedCourseRepository.findCourseIdsByUserId(user.getId());
            List<Long> enrolledCourseIds = enrolledCourseRepository.findCourseIdsByUserId(user.getId());
            
            // 4. 이수/수강중인 과목 제외
            List<Map<String, Object>> mandatoryCourses = new ArrayList<>();
            
            for (TrackRequirement requirement : trackRequirements) {
                Course course = requirement.getCourse();
                Long courseId = course.getId();
                
                // 이미 이수했거나 수강중인 과목은 제외
                if (completedCourseIds.contains(courseId) || enrolledCourseIds.contains(courseId)) {
                    continue;
                }
                
                // 필수 과목 정보 생성
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("courseId", courseId);
                courseInfo.put("courseName", course.getCourseName());
                courseInfo.put("courseCode", course.getCourseCode());
                courseInfo.put("credits", course.getCredits());
                courseInfo.put("openGrade", course.getOpenGrade());
                courseInfo.put("openSemester", course.getOpenSemester());
                courseInfo.put("description", course.getDescription());
                courseInfo.put("courseType", requirement.getCourseType()); // "MANDATORY" 또는 "FOUNDATION"
                courseInfo.put("trackName", requirement.getTrack().getTrackName());
                
                mandatoryCourses.add(courseInfo);
            }

            log.info("1단계: 필수 과목 목록 확보 완료 - {}개 과목", mandatoryCourses.size());
            return mandatoryCourses;

        } catch (Exception e) {
            log.error("1단계: 필수 과목 목록 확보 실패 - trackIds={}, studentId={}", trackIds, studentId, e);
            throw new RuntimeException("필수 과목 목록 확보에 실패했습니다.", e);
        }
    }

    /**
     * 2단계: 벡터 DB 검색 목록 확보 (유사도 검색)
     * 사용자의 트랙과 학습 스타일을 고려하여 Qdrant에서 유사도 검색
     */
    public List<Map<String, Object>> getRecommendedCourses(List<Long> trackIds, String studentId,
                                                          CourseRecommendationRequest.LearningStyle learningStyle,
                                                          CourseRecommendationRequest.AdvancedSettings advancedSettings) {
        try {
            log.info("2단계: 벡터 DB 검색 시작 - trackIds={}, studentId={}, learningStyle={}, advancedSettings={}",
                    trackIds, studentId, learningStyle, advancedSettings);

            // 1. 학번으로 사용자 찾기
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("학번 " + studentId + "에 해당하는 사용자를 찾을 수 없습니다."));

            // 2. 이미 이수한 과목과 수강중인 과목 ID 목록 조회
            List<Long> completedCourseIds = completedCourseRepository.findCourseIdsByUserId(user.getId());
            List<Long> enrolledCourseIds = enrolledCourseRepository.findCourseIdsByUserId(user.getId());
            
            // 3. 검색 쿼리 생성 (tech_stack 우선, 없으면 기본 쿼리)
            String searchQuery;
            if (advancedSettings != null && advancedSettings.getTechStack() != null && !advancedSettings.getTechStack().trim().isEmpty()) {
                searchQuery = advancedSettings.getTechStack();
                log.info("Tech Stack 기반 검색: {}", searchQuery);
            } else {
                searchQuery = "프로그래밍 개발"; // 기본 검색 쿼리
                log.info("기본 검색 쿼리 사용: {}", searchQuery);
            }
            
            // 4. 트랙 이름 목록 조회 (Qdrant 필터링용)
            List<String> trackNames = getTrackNamesByIds(trackIds);
            log.info("트랙 필터링: {}", trackNames);
            
            // 5. Qdrant에서 트랙 필터링된 유사도 검색
            List<Map<String, Object>> searchResults = qdrantRepository.searchSimilarCoursesWithFilter(
                    searchQuery, 30, trackNames);
            log.info("Qdrant 검색 결과: {}개", searchResults.size());
            
            // 6. 이수/수강중인 과목 제외
            List<Map<String, Object>> recommendedCourses = new ArrayList<>();
            
            for (Map<String, Object> course : searchResults) {
                // Qdrant 결과에서 courseId 추출 (문자열로 저장되어 있을 수 있음)
                Object courseIdObj = course.get("courseId");
                Long courseId = null;
                
                if (courseIdObj instanceof Long) {
                    courseId = (Long) courseIdObj;
                } else if (courseIdObj instanceof String) {
                    try {
                        courseId = Long.parseLong((String) courseIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("잘못된 courseId 형식: {}", courseIdObj);
                        continue;
                    }
                } else if (courseIdObj instanceof Integer) {
                    courseId = ((Integer) courseIdObj).longValue();
                }
                
                if (courseId == null) {
                    continue;
                }
                
                // 이미 이수했거나 수강중인 과목은 제외
                if (completedCourseIds.contains(courseId) || enrolledCourseIds.contains(courseId)) {
                    continue;
                }
                
                recommendedCourses.add(course);
            }

            log.info("2단계: 벡터 DB 검색 완료 - {}개 과목 (트랙 필터링 후)", recommendedCourses.size());
            return recommendedCourses;

        } catch (Exception e) {
            log.error("2단계: 벡터 DB 검색 실패 - trackIds={}, studentId={}, learningStyle={}", trackIds, studentId, learningStyle, e);
            throw new RuntimeException("벡터 DB 검색에 실패했습니다.", e);
        }
    }
    
    /**
     * 트랙 ID 목록으로 트랙 이름 목록 조회
     */
    private List<String> getTrackNamesByIds(List<Long> trackIds) {
        try {
            List<TrackRequirement> requirements = trackRequirementRepository.findByTrackIdsAndCourseType(trackIds);
            return requirements.stream()
                    .map(req -> req.getTrack().getTrackName())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("트랙 이름 조회 실패 - trackIds: {}", trackIds, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 주어진 트랙들에 속하는 모든 과목 ID 목록 조회
     */
    private List<Long> getCourseIdsByTracks(List<Long> trackIds) {
        try {
            List<TrackRequirement> requirements = trackRequirementRepository.findByTrackIdsAndCourseType(trackIds);
            return requirements.stream()
                    .map(req -> req.getCourse().getId())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("트랙별 과목 조회 실패 - trackIds: {}", trackIds, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 사용자의 현재 학기 상태 계산
     */
    private Map<String, Object> calculateUserSemester(String studentId) {
        try {
            // 1. 학번으로 사용자 찾기
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("학번 " + studentId + "에 해당하는 사용자를 찾을 수 없습니다."));

            // 2. 이수한 과목 중 가장 최신 학년/학기 찾기
            List<grit.guidance.domain.user.entity.CompletedCourse> completedCourses = 
                    completedCourseRepository.findByUsers(user);
            
            int latestYear = 0;
            String latestSemester = "FIRST";
            
            if (!completedCourses.isEmpty()) {
                for (grit.guidance.domain.user.entity.CompletedCourse completed : completedCourses) {
                    if (completed.getCompletedYear() > latestYear) {
                        latestYear = completed.getCompletedYear();
                        latestSemester = completed.getCompletedSemester().toString();
                    } else if (completed.getCompletedYear() == latestYear) {
                        // 같은 년도면 학기 비교 (SECOND > FIRST)
                        if ("SECOND".equals(completed.getCompletedSemester().toString()) && 
                            "FIRST".equals(latestSemester)) {
                            latestSemester = "SECOND";
                        }
                    }
                }
            }
            
            // 3. 현재 수강중인 과목이 있는지 확인
            List<grit.guidance.domain.user.entity.EnrolledCourse> enrolledCourses = 
                    enrolledCourseRepository.findByUser(user);
            boolean hasCurrentEnrollment = !enrolledCourses.isEmpty();
            
            // 4. 다음 학기 계산
            int nextYear;
            String nextSemester;
            
            if (hasCurrentEnrollment) {
                // 현재 수강중인 과목이 있으면, 현재 수강중인 학기의 다음 학기
                // 최신 이수가 3-1이고 현재 수강중이면 → 현재는 3-2 → 다음은 4-1
                if ("FIRST".equals(latestSemester)) {
                    // 최신 이수가 1학기면, 현재는 2학기 → 다음은 다음년도 1학기
                    nextYear = latestYear + 1;
                    nextSemester = "FIRST";
                } else { // SECOND
                    // 최신 이수가 2학기면, 현재는 다음년도 1학기 → 다음은 다음년도 2학기
                    nextYear = latestYear + 1;
                    nextSemester = "SECOND";
                }
            } else {
                // 현재 수강중인 과목이 없으면 +1학기
                if ("FIRST".equals(latestSemester)) {
                    nextYear = latestYear;
                    nextSemester = "SECOND";
                } else { // SECOND
                    nextYear = latestYear + 1;
                    nextSemester = "FIRST";
                }
            }
            
            Map<String, Object> semesterInfo = new HashMap<>();
            semesterInfo.put("latestCompletedYear", latestYear);
            semesterInfo.put("latestCompletedSemester", latestSemester);
            semesterInfo.put("hasCurrentEnrollment", hasCurrentEnrollment);
            semesterInfo.put("nextYear", nextYear);
            semesterInfo.put("nextSemester", nextSemester);
            semesterInfo.put("recommendationStartYear", nextYear);
            semesterInfo.put("recommendationStartSemester", nextSemester);
            
            log.info("학기 계산 완료 - 최신이수: {}{}, 현재수강: {}, 추천시작: {}{}",
                    latestYear, latestSemester, hasCurrentEnrollment, nextYear, nextSemester);
            
            return semesterInfo;
            
        } catch (Exception e) {
            log.error("사용자 학기 계산 실패 - studentId: {}", studentId, e);
            throw new RuntimeException("사용자 학기 계산에 실패했습니다.", e);
        }
    }
    
    /**
     * 특정 과목이 주어진 트랙들에 속하는지 확인
     */
    private boolean isCourseInTracks(Long courseId, List<Long> trackIds) {
        try {
            List<TrackRequirement> requirements = trackRequirementRepository.findByCourseId(courseId);
            return requirements.stream()
                    .anyMatch(req -> trackIds.contains(req.getTrack().getId()));
        } catch (Exception e) {
            log.warn("트랙 확인 실패 - courseId: {}, trackIds: {}", courseId, trackIds, e);
            return false;
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

    /**
     * 통합 로드맵 추천 (1단계 + 2단계 + LLM)
     */
    public Map<String, Object> getIntegratedRoadmapRecommendation(
            List<Long> trackIds, 
            String studentId,
            CourseRecommendationRequest.LearningStyle learningStyle,
            CourseRecommendationRequest.AdvancedSettings advancedSettings) {
        
        try {
            log.info("통합 로드맵 추천 시작 - trackIds={}, studentId={}", trackIds, studentId);

            // 0단계: 사용자 학기 상태 파악
            Map<String, Object> semesterInfo = calculateUserSemester(studentId);
            log.info("사용자 학기 정보: {}", semesterInfo);
            
            // 1단계: 필수 과목 목록 확보 (규칙 기반 필터링)
            List<Map<String, Object>> mandatoryCourses = getMandatoryCourses(trackIds, studentId);
            log.info("1단계 완료: 필수 과목 {}개", mandatoryCourses.size());
            
            // 2단계: 벡터 DB 검색 목록 확보 (유사도 검색)
            List<Map<String, Object>> recommendedCourses = getRecommendedCourses(
                    trackIds, studentId, learningStyle, advancedSettings);
            log.info("2단계 완료: 추천 과목 {}개", recommendedCourses.size());
            
            // 3단계: LLM에게 로드맵 추천 요청 (학기 정보 포함)
            String techStack = (advancedSettings != null && advancedSettings.getTechStack() != null) 
                    ? advancedSettings.getTechStack() : "";
            
            Map<String, Object> llmResponse = llmRoadmapService.generateRoadmapRecommendation(
                    studentId, trackIds, mandatoryCourses, recommendedCourses, techStack, semesterInfo);
            
            // 4단계: 최종 응답 구성 (LLM 추천 결과만 반환)
            Map<String, Object> response = new HashMap<>();
            response.put("studentId", studentId);
            response.put("trackIds", trackIds);
            response.put("semesterInfo", semesterInfo);
            response.put("roadMap", llmResponse.get("roadMap"));
            response.put("status", "success");
            
            log.info("통합 로드맵 추천 완료");
            return response;

        } catch (Exception e) {
            log.error("통합 로드맵 추천 실패 - trackIds={}, studentId={}", trackIds, studentId, e);
            throw new RuntimeException("통합 로드맵 추천에 실패했습니다.", e);
        }
    }
}
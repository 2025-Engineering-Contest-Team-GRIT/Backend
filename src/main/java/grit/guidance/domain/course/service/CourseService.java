package grit.guidance.domain.course.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import grit.guidance.domain.course.dto.CourseDataDto;
import grit.guidance.domain.course.entity.*;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.course.repository.CoursePrerequisiteRepository;
import grit.guidance.domain.course.repository.TrackRepository;
import grit.guidance.domain.course.repository.TrackRequirementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CoursePrerequisiteRepository coursePrerequisiteRepository;
    private final TrackRepository trackRepository;
    private final TrackRequirementRepository trackRequirementRepository;
    private final ObjectMapper objectMapper;

    /**
     * 모든 과목 조회
     */
    public List<Course> findAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional
    public void initializeDatabaseFromJson() {
        log.info("관리자 요청으로 데이터베이스 초기화를 시작합니다...");

        try {
            // 주의: Track은 수동으로 넣었으므로 삭제하지 않음
            // 소프트 딜리트를 위해 @Modifying @Query 사용
            coursePrerequisiteRepository.deleteAllSoft();
            trackRequirementRepository.deleteAllSoft();
            courseRepository.deleteAllSoft();
            log.info("기존 과목, 선수과목 관계 및 이수 요건 데이터를 모두 소프트 삭제했습니다.");

            // 1. JSON 파일 파싱
            ClassPathResource resource = new ClassPathResource("data/courses.json");
            InputStream inputStream = resource.getInputStream();
            List<CourseDataDto> dtoList = objectMapper.readValue(inputStream, new TypeReference<>() {});
            log.info("{}개의 레코드를 JSON 파일에서 읽었습니다.", dtoList.size());

            // 2. DB에서 기존 트랙 정보 불러오기
            Map<String, Track> trackMap = getTrackMap(dtoList);
            log.info("{}개의 트랙 정보를 DB에서 성공적으로 불러왔습니다.", trackMap.size());

            // 3. 과목 정보 저장
            Map<String, Course> courseMap = createAndSaveCourses(dtoList);
            log.info("{}개의 과목 정보를 DB에 성공적으로 저장했습니다.", courseMap.size());

            // 4. 이수 요건 정보 저장
            createAndSaveRequirements(dtoList, courseMap, trackMap);
            log.info("트랙별 이수 요건 정보를 성공적으로 저장했습니다.");

            // 5. 선수과목 관계 저장
            createAndSavePrerequisites(dtoList, courseMap);
            log.info("선수과목 관계 정보를 성공적으로 저장했습니다.");

            log.info("데이터베이스 초기화를 성공적으로 완료했습니다.");

        } catch (Exception e) {
            log.error("데이터베이스 초기화 중 오류가 발생했습니다.", e);
            throw new RuntimeException("데이터 초기화에 실패했습니다.", e);
        }
    }

     // JSON 데이터에 명시된 모든 트랙 이름을 DB에서 조회하여 Map<트랙이름, Track엔티티> 형태로 반환
     // DB에 해당 트랙이 존재하지 않으면 예외를 발생

    private Map<String, Track> getTrackMap(List<CourseDataDto> dtoList) {
        // JSON 파일에 있는 모든 트랙 이름을 중복 없이 추출
        List<String> trackNames = dtoList.stream()
                .map(CourseDataDto::getTrack)
                .distinct()
                .toList();

        // 각 트랙 이름으로 DB에서 Track 엔티티를 찾아 Map으로 만듦
        return trackNames.stream()
                .collect(Collectors.toMap(
                        Function.identity(), // key는 트랙 이름
                        trackName -> trackRepository.findByTrackName(trackName) // value는 DB에서 찾은 Track 엔티티
                                .orElseThrow(() -> new IllegalStateException("DB에 '" + trackName + "' 트랙이 존재하지 않습니다. 수동으로 추가해주세요."))
                ));
    }

    /**
     * JSON 데이터에서 중복을 제거한 모든 과목(Course)을 DB에 저장하고,
     * Map<과목코드, Course엔티티> 형태로 반환합니다.
     */
    private Map<String, Course> createAndSaveCourses(List<CourseDataDto> dtoList) {
        // 과목별로 그룹화 (중복 제거)
        Map<String, List<CourseDataDto>> courseGroups = dtoList.stream()
                .collect(Collectors.groupingBy(CourseDataDto::getId));

        // 각 과목 그룹을 하나의 Course 엔티티로 변환
        List<Course> courses = new ArrayList<>();
        for (Map.Entry<String, List<CourseDataDto>> entry : courseGroups.entrySet()) {
            List<CourseDataDto> courseDataList = entry.getValue();

            // 첫 번째 데이터로 기본 정보 설정
            CourseDataDto firstData = courseDataList.get(0);
            Semester semester = convertStringToSemester(firstData.getSemester());
            Course course = Course.builder()
                    .courseCode(firstData.getId())
                    .courseName(firstData.getName())
                    .openGrade(firstData.getYear())
                    .openSemester(semester)
                    .credits(firstData.getCredits())
                    .description(firstData.getDescription())
                    .build();

            courses.add(course);
        }

        // 중복 제거된 Course 리스트를 DB에 한번에 저장
        courseRepository.saveAll(courses);
        
        // 강제로 플러시하여 ID 생성 보장
        courseRepository.flush();
        log.info("Course 엔티티 저장 및 플러시 완료");

        // 나중에 쉽게 찾아 쓸 수 있도록 Map 형태로 반환
        return courses.stream()
                .collect(Collectors.toMap(Course::getCourseCode, Function.identity()));
    }


    // 전체 JSON 데이터를 바탕으로 과목과 트랙을 연결하는 TrackRequirement를 생성하고 DB에 저장
    private void createAndSaveRequirements(List<CourseDataDto> dtoList, Map<String, Course> courseMap, Map<String, Track> trackMap) {
        // DTO 리스트를 순회하며 TrackRequirement 엔티티 리스트를 생성
        List<TrackRequirement> requirements = dtoList.stream()
                .map(dto -> {
                    Course course = courseMap.get(dto.getId());
                    Track track = trackMap.get(dto.getTrack());
                    CourseType courseType = convertStringToCourseType(dto.getCategory());

                    return TrackRequirement.builder()
                            .course(course)
                            .track(track)
                            .courseType(courseType)
                            .build();
                })
                .toList();

        // 모든 Requirement를 DB에 한번에 저장
        trackRequirementRepository.saveAll(requirements);
    }

    // 학기 문자열("1", "2")을 Semester Enum으로 변환
    private Semester convertStringToSemester(String semesterStr) {
        return "1".equals(semesterStr) ? Semester.FIRST : Semester.SECOND;
    }


    //이수구분 문자열("전필", "전선"...)을 CourseType Enum으로 변환
    private CourseType convertStringToCourseType(String categoryStr) {
        return switch (categoryStr) {
            case "전필" -> CourseType.MANDATORY;
            case "전선" -> CourseType.ELECTIVE;
            case "전기" -> CourseType.FOUNDATION;
            default -> CourseType.GENERAL_ELECTIVE;
        };
    }

    /**
     * JSON 데이터에서 선수과목 관계를 추출하여 CoursePrerequisite 엔티티를 생성하고 DB에 저장
     */
    private void createAndSavePrerequisites(List<CourseDataDto> dtoList, Map<String, Course> courseMap) {
        log.info("선수과목 관계 저장 시작 - 총 {}개의 과목 데이터 처리", dtoList.size());
        
        List<CoursePrerequisite> prerequisites = new ArrayList<>();
        int totalPrerequisites = 0;
        int processedCourses = 0;
        int skippedCourses = 0;
        int missingPrerequisites = 0;
        int loopCount = 0;
        
        for (CourseDataDto dto : dtoList) {
            loopCount++;
            log.debug("루프 {}번째 - 과목 ID: {}", loopCount, dto.getId());
            log.debug("prerequisiteIds 값: {}", dto.getPrerequisiteIds());
            // 선수과목 ID 목록이 있는 경우에만 처리
            if (dto.getPrerequisiteIds() != null && !dto.getPrerequisiteIds().isEmpty()) {
                log.info("과목 '{}'의 선수과목: {}", dto.getId(), dto.getPrerequisiteIds());
                totalPrerequisites += dto.getPrerequisiteIds().size();
                processedCourses++;
                
                Course course = courseMap.get(dto.getId());
                if (course != null) {
                    log.debug("과목 '{}' (ID: {}) 처리 중", dto.getId(), course.getId());
                    if (course.getId() == null) {
                        log.error("Course 엔티티의 ID가 null입니다! 과목: {}", dto.getId());
                        continue;
                    }
                    
                    for (String prerequisiteId : dto.getPrerequisiteIds()) {
                        // 선수과목 ID로 Course 엔티티 찾기
                        Course prerequisite = courseMap.get(prerequisiteId);
                        if (prerequisite != null) {
                            log.debug("선수과목 '{}' (ID: {}) 찾음", prerequisiteId, prerequisite.getId());
                            
                            // 중복 관계 확인
                            if (!coursePrerequisiteRepository.existsByCourseIdAndPrerequisiteId(
                                    course.getId(), prerequisite.getId())) {
                                CoursePrerequisite coursePrerequisite = CoursePrerequisite.builder()
                                        .course(course)
                                        .prerequisiteId(prerequisite.getId())
                                        .build();
                                prerequisites.add(coursePrerequisite);
                                log.debug("선수과목 관계 추가: {} -> {}", course.getId(), prerequisite.getId());
                            } else {
                                log.debug("이미 존재하는 선수과목 관계: {} -> {}", course.getId(), prerequisite.getId());
                            }
                        } else {
                            log.warn("선수과목 ID '{}'에 해당하는 과목을 찾을 수 없습니다.", prerequisiteId);
                            missingPrerequisites++;
                        }
                    }
                } else {
                    log.warn("과목 ID '{}'에 해당하는 Course 엔티티를 찾을 수 없습니다.", dto.getId());
                    skippedCourses++;
                }
            } else {
                log.debug("과목 '{}'에는 선수과목이 없습니다.", dto.getId());
            }
        }
        
        log.info("선수과목 관계 처리 완료:");
        log.info("- 총 루프 실행 횟수: {}", loopCount);
        log.info("- 선수과목이 있는 과목 수: {}", processedCourses);
        log.info("- 총 선수과목 ID 수: {}", totalPrerequisites);
        log.info("- 생성된 관계 수: {}", prerequisites.size());
        log.info("- 찾을 수 없는 선수과목 수: {}", missingPrerequisites);
        log.info("- 처리 실패한 과목 수: {}", skippedCourses);
        
        // 모든 선수과목 관계를 DB에 한번에 저장
        if (!prerequisites.isEmpty()) {
            coursePrerequisiteRepository.saveAll(prerequisites);
            log.info("{}개의 선수과목 관계를 데이터베이스에 저장했습니다.", prerequisites.size());
        } else {
            log.warn("저장할 선수과목 관계가 없습니다.");
        }
    }

}
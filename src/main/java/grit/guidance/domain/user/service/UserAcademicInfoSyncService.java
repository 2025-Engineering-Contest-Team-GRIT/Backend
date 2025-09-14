package grit.guidance.domain.user.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.course.entity.Track;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.course.repository.TrackRepository;
import grit.guidance.domain.user.dto.HansungDataResponse;
import grit.guidance.domain.user.dto.SemesterGradeResponse;
import grit.guidance.domain.user.entity.*;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAcademicInfoSyncService {

    private final UsersCrawlingService crawlingService;
    private final UsersRepository usersRepository;
    private final CourseRepository courseRepository;
    private final TrackRepository trackRepository;
    private final UserTrackRepository userTrackRepository;
    private final CompletedCourseRepository completedCourseRepository;

    @Transactional
    public void syncHansungInfo(String studentId, String password) {
        try {
            // 1. 크롤링 서비스 호출하여 데이터 가져오기
            log.info("{} 학번 학생의 정보 크롤링을 시작합니다.", studentId);
            HansungDataResponse crawledData = crawlingService.fetchHansungData(studentId, password);
            log.info("{} 학생({})의 정보 크롤링을 완료했습니다.", crawledData.userInfo().name(), studentId);

            // 2. 사용자 정보 찾기 또는 신규 생성
            Users users = usersRepository.findByStudentId(studentId)
                    .orElseGet(() -> usersRepository.save(Users.builder().studentId(studentId).build()));

            // 3. 사용자 정보(GPA) 업데이트
            users.updateGpa(parseGpa(crawledData.grades().creditSummary()));
            log.info("{} 학생의 GPA 정보를 {}로 업데이트했습니다.", users.getStudentId(), users.getGpa());

            // 4. 기존 이수 내역 및 트랙 정보 삭제 (최신 정보로 덮어쓰기 위해)
            completedCourseRepository.deleteByUsers(users);
            userTrackRepository.deleteByUsers(users);
            log.info("{} 학생의 기존 이수 내역 및 트랙 정보를 삭제했습니다.", users.getStudentId());

            // 5. 새로 크롤링한 트랙 정보 저장
            saveUserTracks(users, crawledData.userInfo().tracks());

            // 6. 새로 크롤링한 이수 과목 정보 저장
            saveCompletedCourses(users, crawledData.grades().semesters());

            log.info("{} 학생 정보 동기화 완료", studentId);

        } catch (Exception e) {
            log.error("{} 학생 정보 동기화 중 오류 발생", studentId, e);
            throw new RuntimeException("사용자 정보 동기화에 실패했습니다.", e);
        }
    }

    private void saveUserTracks(Users users, List<String> crawledTrackNames) {
        List<UserTrack> newUserTracks = new ArrayList<>();
        if (!crawledTrackNames.isEmpty()) {
            // 첫 번째 트랙을 PRIMARY로 설정
            Track primaryTrack = trackRepository.findByTrackName(crawledTrackNames.get(0))
                    .orElseThrow(() -> new IllegalStateException("DB에 존재하지 않는 트랙입니다: " + crawledTrackNames.get(0)));
            newUserTracks.add(UserTrack.builder().users(users).track(primaryTrack).trackType(TrackType.PRIMARY).build());

            // 두 번째 트랙이 있다면 SECONDARY로 설정
            if (crawledTrackNames.size() > 1) {
                Track secondaryTrack = trackRepository.findByTrackName(crawledTrackNames.get(1))
                        .orElseThrow(() -> new IllegalStateException("DB에 존재하지 않는 트랙입니다: " + crawledTrackNames.get(1)));
                newUserTracks.add(UserTrack.builder().users(users).track(secondaryTrack).trackType(TrackType.SECONDARY).build());
            }
        }
        userTrackRepository.saveAll(newUserTracks);
        log.info("{} 학생의 트랙 정보를 {}개 저장했습니다.", users.getStudentId(), newUserTracks.size());
    }

    private void saveCompletedCourses(Users user, List<SemesterGradeResponse> semesters) {
        List<CompletedCourse> newCompletedCourses = semesters.stream()
                .flatMap(semester -> semester.courses().stream()
                        .map(courseGrade -> {
                            Course course = courseRepository.findByCourseCode(courseGrade.code()).orElse(null);
                            if (course == null) {
                                log.warn("DB에 존재하지 않는 과목 코드입니다: {}({}). 이수 내역에 추가하지 않습니다.", courseGrade.name(), courseGrade.code());
                                return null;
                            }

                            CompletedGrade gradeEnum = parseGradeFromString(courseGrade.grade());
                            
                            // 트랙 정보 파싱 (null일 수 있음)
                            Track track = null;
                            try {
                                track = parseTrackFromTrackStatus(courseGrade.trackStatus());
                            } catch (Exception e) {
                                log.warn("트랙 파싱 실패: {}. null로 설정합니다.", e.getMessage());
                            }

                            // CompletedCourse 엔티티 생성
                            return CompletedCourse.builder()
                                    .users(user)
                                    .course(course)
                                    .track(track)
                                    .completedYear(parseYearFromSemesterString(semester.semester()))
                                    .gradeLevel(course.getOpenGrade()) // 이수학년은 과목의 개설학년으로 저장
                                    .completedSemester(parseSemesterFromSemesterString(semester.semester()))
                                    .completedGrade(gradeEnum)
                                    .gradePoint(gradeEnum.getGradePoint())
                                    .build();
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        completedCourseRepository.saveAll(newCompletedCourses);
        log.info("{} 학생의 이수 과목 정보를 {}개 저장했습니다.", user.getStudentId(), newCompletedCourses.size());
    }

    private BigDecimal parseGpa(Map<String, String> creditSummary) {
        String gpaString = creditSummary.getOrDefault("평균평점", "0.0");
        try {
            return new BigDecimal(gpaString);
        } catch (NumberFormatException e) {
            log.warn("평균평점 '{}'을 숫자로 변환할 수 없습니다. 0.0으로 처리합니다.", gpaString);
            return BigDecimal.ZERO;
        }
    }

    private Integer parseYearFromSemesterString(String semesterString) {
        if (semesterString == null || semesterString.length() < 4) {
            return 0; // 예외 처리
        }
        // "2024학년도 1학기" -> 2024
        return Integer.parseInt(semesterString.substring(0, 4));
    }

    private Semester parseSemesterFromSemesterString(String semesterString) {
        if (semesterString == null) {
            return Semester.FIRST; // 기본값
        }
        // 공백 제거
        String processedString = semesterString.replaceAll("\\s+", "");

        if (processedString.contains("여름학기")) {
            return Semester.SUMMER;
        }
        if (processedString.contains("겨울학기")) {
            return Semester.WINTER;
        }
        if (processedString.contains("2학기")) {
            return Semester.SECOND;
        }

        return Semester.FIRST;
    }

    private CompletedGrade parseGradeFromString(String gradeString) {
        return switch (gradeString) {
            case "A+" -> CompletedGrade.A_PLUS;
            case "A0", "A" -> CompletedGrade.A;
            case "B+" -> CompletedGrade.B_PLUS;
            case "B0", "B" -> CompletedGrade.B;
            case "C+" -> CompletedGrade.C_PLUS;
            case "C0", "C" -> CompletedGrade.C;
            case "D+" -> CompletedGrade.D_PLUS;
            case "D0", "D" -> CompletedGrade.D;
            case "P", "Pass" -> CompletedGrade.PASS;
            case "F", "Fail" -> CompletedGrade.F;
            default -> {
                log.warn("알 수 없는 성적'{}'을 F로 처리합니다.", gradeString);
                yield CompletedGrade.F;
            }
        };
    }

    private Track parseTrackFromTrackStatus(String trackStatus) {
        if (trackStatus == null || trackStatus.trim().isEmpty()) {
            log.warn("트랙 상태가 비어있습니다. null을 반환합니다.");
            return null;
        }

        // 이미 크롤링 서비스에서 정리된 트랙명을 받음
        // "제1트랙", "제2트랙", "" 등의 형태
        if ("제1트랙".equals(trackStatus)) {
            return trackRepository.findByTrackName("제1트랙").orElse(null);
        } else if ("제2트랙".equals(trackStatus)) {
            return trackRepository.findByTrackName("제2트랙").orElse(null);
        } else {
            // 빈 문자열이거나 알 수 없는 경우 null 반환
            return null;
        }
    }
}
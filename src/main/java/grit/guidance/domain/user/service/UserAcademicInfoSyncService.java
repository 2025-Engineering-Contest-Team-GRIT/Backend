package grit.guidance.domain.user.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.course.entity.Track;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.course.repository.TrackRepository;
import grit.guidance.domain.graduation.entity.CrawlingGraduation;
import grit.guidance.domain.graduation.repository.CrawlingGraduationRepository;
import grit.guidance.domain.user.dto.HansungDataResponse;
import grit.guidance.domain.user.dto.MajorRequiredCreditsResponse;
import grit.guidance.domain.user.dto.SemesterGradeResponse;
import grit.guidance.domain.user.dto.CourseGradeResponse;
import grit.guidance.domain.user.entity.*;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.GraduationRequirementRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;
import grit.guidance.domain.user.entity.TrackType;
import grit.guidance.domain.graduation.repository.CrawlingGraduationRepository;
import grit.guidance.domain.user.repository.GraduationRequirementRepository;

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
    private final CrawlingGraduationRepository crawlingGraduationRepository;
    private final GraduationRequirementRepository graduationRequirementRepository;

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

            // 3. 사용자 정보(GPA, 취득학점, 시간표) 업데이트
            users.updateGpa(parseGpa(crawledData.grades().creditSummary()));
            users.updateEarnedCredits(parseEarnedCredits(crawledData.grades().creditSummary()));
            users.updateTimetable(crawledData.timetableJson());
            users.updateLastCrawlTime();
            log.info("{} 학생의 GPA 정보를 {}로 업데이트했습니다.", users.getStudentId(), users.getGpa());
            log.info("{} 학생의 취득학점을 {}로 업데이트했습니다.", users.getStudentId(), users.getEarnedCredits());
            log.info("{} 학생의 시간표 정보를 업데이트했습니다.", users.getStudentId());

            // 4. 기존 이수 내역 및 트랙 정보 삭제 (최신 정보로 덮어쓰기 위해)
            completedCourseRepository.deleteByUsers(users);
            userTrackRepository.deleteByUsers(users);
            log.info("{} 학생의 기존 이수 내역 및 트랙 정보를 삭제했습니다.", users.getStudentId());

            // 5. 새로 크롤링한 트랙 정보 저장
            saveUserTracks(users, crawledData.userInfo().tracks());

            // 6. 새로 크롤링한 이수 과목 정보 저장
            saveCompletedCourses(users, crawledData.grades().semesters());


            // ⭐ CrawlingGraduation 저장 로직 추가
            saveOrUpdateCrawlingGraduationData(users, crawledData);

            // ⭐ GraduationRequirement 저장 로직 추가
            saveOrUpdateGraduationRequirement(users);

            log.info("{} 학생 정보 동기화 완료", studentId);

            log.info("{} 학생 정보 동기화 완료", studentId);

        } catch (Exception e) {
            log.error("{} 학생 정보 동기화 중 오류 발생", studentId, e);
            throw new RuntimeException("사용자 정보 동기화에 실패했습니다.", e);
        }
    }

    @Transactional
    public void saveOrUpdateCrawlingGraduationData(Users user, HansungDataResponse crawledData) {
        MajorRequiredCreditsResponse majorCredits = crawledData.majorCredits();
        Integer totalCompletedCredits = parseEarnedCredits(crawledData.grades().creditSummary());

        Optional<grit.guidance.domain.graduation.entity.CrawlingGraduation> existingData = crawlingGraduationRepository.findByUsers(user);

        if (existingData.isPresent()) {
            CrawlingGraduation data = existingData.get();
            data.updateCrawlingData(
                    totalCompletedCredits,
                    Integer.parseInt(majorCredits.getTrack1().getMajorBasic()),
                    Integer.parseInt(majorCredits.getTrack1().getMajorRequired()),
                    Integer.parseInt(majorCredits.getTrack1().getMajorSubtotal()),
                    Integer.parseInt(majorCredits.getTrack2().getMajorBasic()),
                    Integer.parseInt(majorCredits.getTrack2().getMajorRequired()),
                    Integer.parseInt(majorCredits.getTrack2().getMajorSubtotal()),
                    Integer.parseInt(majorCredits.getTotal().getCompleted()),
                    Integer.parseInt(majorCredits.getTotal().getRequired().replaceAll("\\D", ""))
            );
        } else {
            CrawlingGraduation newData = CrawlingGraduation.builder()
                    .users(user)
                    .totalCompletedCredits(totalCompletedCredits)
                    .track1MajorBasic(Integer.parseInt(majorCredits.getTrack1().getMajorBasic()))
                    .track1MajorRequired(Integer.parseInt(majorCredits.getTrack1().getMajorRequired()))
                    .track1MajorSubtotal(Integer.parseInt(majorCredits.getTrack1().getMajorSubtotal()))
                    .track2MajorBasic(Integer.parseInt(majorCredits.getTrack2().getMajorBasic()))
                    .track2MajorRequired(Integer.parseInt(majorCredits.getTrack2().getMajorRequired()))
                    .track2MajorSubtotal(Integer.parseInt(majorCredits.getTrack2().getMajorSubtotal()))
                    .totalMajorCompleted(Integer.parseInt(majorCredits.getTotal().getCompleted()))
                    .totalMajorRequired(Integer.parseInt(majorCredits.getTotal().getRequired().replaceAll("\\D", "")))
                    .build();
            crawlingGraduationRepository.save(newData);
        }
        log.info("{} 학생의 크롤링된 졸업 학점 정보를 저장/업데이트했습니다.", user.getStudentId());
    }

    // ⭐ GraduationRequirement 저장/업데이트 메서드
    @Transactional
    public void saveOrUpdateGraduationRequirement(Users user) {
        // 이미 데이터가 존재하면 아무 작업도 하지 않음
        if (graduationRequirementRepository.findByUsers(user).isEmpty()) {
            GraduationRequirement newRequirement = GraduationRequirement.builder()
                    .users(user)
                    .capstoneCompleted(false)
                    .thesisSubmitted(false)
                    .awardOrCertificateReceived(false)
                    .build();
            graduationRequirementRepository.save(newRequirement);
            log.info("{} 학생의 졸업 요건 정보를 새로 생성했습니다.", user.getStudentId());
        }
    }


    // 이 메서드는 기존대로 유지
// UserAcademicInfoSyncService.java의 saveUserTracks 메서드
    private void saveUserTracks(Users users, List<String> crawledTrackNames) {
        List<UserTrack> newUserTracks = new ArrayList<>();
        if (!crawledTrackNames.isEmpty()) {
            // 첫 번째 트랙을 PRIMARY로 설정
            Track primaryTrack = trackRepository.findByTrackName(crawledTrackNames.get(0))
                    .orElseThrow(() -> new IllegalStateException("DB에 존재하지 않는 트랙입니다: " + crawledTrackNames.get(0)));
            // ⭐ 수정: UserTrack.TrackType -> TrackType
            newUserTracks.add(UserTrack.builder().users(users).track(primaryTrack).trackType(TrackType.PRIMARY).build());

            // 두 번째 트랙이 있다면 SECONDARY로 설정
            if (crawledTrackNames.size() > 1) {
                Track secondaryTrack = trackRepository.findByTrackName(crawledTrackNames.get(1))
                        .orElseThrow(() -> new IllegalStateException("DB에 존재하지 않는 트랙입니다: " + crawledTrackNames.get(1)));
                // ⭐ 수정: UserTrack.TrackType -> TrackType
                newUserTracks.add(UserTrack.builder().users(users).track(secondaryTrack).trackType(TrackType.SECONDARY).build());
            }
        }
        userTrackRepository.saveAll(newUserTracks);
        log.info("{} 학생의 트랙 정보를 {}개 저장했습니다.", users.getStudentId(), newUserTracks.size());
    }

    // ⭐ saveCompletedCourses 메서드 수정
    private void saveCompletedCourses(Users user, List<SemesterGradeResponse> semesters) {
        List<CompletedCourse> newCompletedCourses = semesters.stream()
                .flatMap(semester -> semester.courses().stream()
                        .map(courseGrade -> {
                            Optional<Course> courseOpt = courseRepository.findByCourseCode(courseGrade.code());
                            if (courseOpt.isPresent()) {
                                Course course = courseOpt.get();

                                // ⭐⭐⭐ 트랙 상태 문자열을 기반으로 DB에 저장된 실제 트랙 엔티티를 찾아서 연결 ⭐⭐⭐
                                Track track = null;
                                if (!courseGrade.trackStatus().isEmpty()) {
                                    // 크롤링 서비스에서 "제1트랙" 또는 "제2트랙"으로 정리된 트랙명을 사용
                                    String trackStatus = courseGrade.trackStatus();

                                    // 사용자의 실제 트랙 이름과 매핑
                                    // 예: "제1트랙" -> "모바일소프트웨어트랙"
                                    String actualTrackName = getActualTrackNameFromStatus(user.getStudentId(), trackStatus);
                                    if (actualTrackName != null) {
                                        track = trackRepository.findByTrackName(actualTrackName).orElse(null);
                                    }

                                    if (track == null) {
                                        log.warn("DB에 존재하지 않는 트랙 이름입니다: {}({}). 해당 트랙 정보를 저장하지 않습니다.",
                                                trackStatus, course.getCourseName());
                                    }
                                }

                                CompletedGrade gradeEnum = parseGradeFromString(courseGrade.grade());

                                // CompletedCourse 엔티티 생성
                                return CompletedCourse.builder()
                                        .users(user)
                                        .course(course)
                                        .track(track) // 파싱된 Track 엔티티를 저장
                                        .completedYear(parseYearFromSemesterString(semester.semester()))
                                        .gradeLevel(course.getOpenGrade()) // 이수학년은 과목의 개설학년으로 저장
                                        .completedSemester(parseSemesterFromSemesterString(semester.semester()))
                                        .completedGrade(gradeEnum)
                                        .gradePoint(gradeEnum.getGradePoint())
                                        .build();
                            } else {
                                log.warn("DB에 존재하지 않는 과목 코드입니다: {}({}). 이수 내역에 추가하지 않습니다.",
                                        courseGrade.name(), courseGrade.code());
                                return null;
                            }
                        }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        completedCourseRepository.saveAll(newCompletedCourses);
        log.info("{} 학생의 이수 과목 정보를 {}개 저장했습니다.", user.getStudentId(), newCompletedCourses.size());
    }

    private BigDecimal parseGpa(Map<String, String> creditSummary) {
        // ... 기존 로직과 동일
        String gpaString = creditSummary.getOrDefault("평균평점", "0.0");
        try {
            return new BigDecimal(gpaString);
        } catch (NumberFormatException e) {
            log.warn("평균평점 '{}'을 숫자로 변환할 수 없습니다. 0.0으로 처리합니다.", gpaString);
            return BigDecimal.ZERO;
        }
    }

    private Integer parseEarnedCredits(Map<String, String> creditSummary) {
        String earnedCreditsString = creditSummary.getOrDefault("취득학점", "0");
        try {
            return Integer.parseInt(earnedCreditsString);
        } catch (NumberFormatException e) {
            log.warn("취득학점 '{}'을 숫자로 변환할 수 없습니다. 0으로 처리합니다.", earnedCreditsString);
            return 0;
        }
    }

    private Integer parseYearFromSemesterString(String semesterString) {
        // ... 기존 로직과 동일
        if (semesterString == null || semesterString.length() < 4) {
            return 0;
        }
        return Integer.parseInt(semesterString.substring(0, 4));
    }

    private Semester parseSemesterFromSemesterString(String semesterString) {
        // ... 기존 로직과 동일
        if (semesterString == null) {
            return Semester.FIRST;
        }
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
        // ... 기존 로직과 동일
        return switch (gradeString) {
            case "A+" -> CompletedGrade.A_PLUS;
            case "A0", "A" -> CompletedGrade.A;
            case "B+" -> CompletedGrade.B_PLUS;
            case "B0", "B" -> CompletedGrade.B;
            case "C+" -> CompletedGrade.C_PLUS;
            case "C0", "C" -> CompletedGrade.C;
            case "D+" -> CompletedGrade.D_PLUS;
            case "D0", "D" -> CompletedGrade.D;
            case "P", "Pass", "인정" -> CompletedGrade.PASS; // "인정" 추가
            case "F", "Fail" -> CompletedGrade.F;
            default -> {
                log.warn("알 수 없는 성적'{}'을 F로 처리합니다.", gradeString);
                yield CompletedGrade.F;
            }
        };
    }

    // ⭐⭐ 새로 추가된 로직: "제1트랙"을 실제 트랙 이름으로 변환 ⭐⭐
    private String getActualTrackNameFromStatus(String studentId, String trackStatus) {
        Users user = usersRepository.findByStudentId(studentId).orElse(null);
        if (user == null) {
            return null;
        }

        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);

        if ("제1트랙".equals(trackStatus)) {
            // PRIMARY 트랙 이름 반환
            return userTracks.stream()
                    .filter(ut -> ut.getTrackType() == TrackType.PRIMARY) // ⭐ 이 부분 수정: UserTrack.TrackType -> TrackType
                    .findFirst()
                    .map(ut -> ut.getTrack().getTrackName())
                    .orElse(null);
        } else if ("제2트랙".equals(trackStatus)) {
            // SECONDARY 트랙 이름 반환
            return userTracks.stream()
                    .filter(ut -> ut.getTrackType() == TrackType.SECONDARY) // ⭐ 이 부분 수정: UserTrack.TrackType -> TrackType
                    .findFirst()
                    .map(ut -> ut.getTrack().getTrackName())
                    .orElse(null);
        }
        return null;
    }
}
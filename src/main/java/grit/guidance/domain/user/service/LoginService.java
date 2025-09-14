package grit.guidance.domain.user.service;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.course.entity.Track;
import grit.guidance.domain.course.repository.CourseRepository;
import grit.guidance.domain.course.repository.TrackRepository;
import grit.guidance.domain.user.dto.*;
import grit.guidance.domain.user.entity.*;
import grit.guidance.domain.user.repository.CompletedCourseRepository;
import grit.guidance.domain.user.repository.EnrolledCourseRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.global.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsersCrawlingService crawlingService;
    private final UsersRepository usersRepository;
    private final UserTrackRepository userTrackRepository;
    private final CompletedCourseRepository completedCourseRepository;
    private final EnrolledCourseRepository enrolledCourseRepository;
    private final CourseRepository courseRepository;
    private final TrackRepository trackRepository;
    private final JwtService jwtService;
    private final CrawlingConditionService crawlingConditionService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 입력값 검증
        if (request.studentId() == null || request.studentId().trim().isEmpty() ||
            request.password() == null || request.password().trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이름/비밀번호를 작성해주세요.");
        }

        try {
            String studentId = request.studentId().trim();
            String password = request.password().trim();
            
            // 2. 기존 사용자 조회
            Users existingUser = usersRepository.findByStudentId(studentId).orElse(null);
            
            // 3. 크롤링 필요 여부 확인
            boolean shouldCrawl = crawlingConditionService.shouldCrawl(existingUser);
            
            if (shouldCrawl) {
                // 4. 크롤링이 필요한 경우에만 크롤링 실행
                log.info("크롤링 실행: studentId={}", studentId);
                HansungDataResponse hansungData = crawlingService.fetchHansungData(studentId, password);
                
                // 5. 크롤링 성공 시 사용자 정보 저장/업데이트
                saveOrUpdateUser(studentId, hansungData);
            } else {
                log.info("크롤링 생략: studentId={}", studentId);
                // 크롤링이 필요 없는 경우 기존 사용자 정보로 로그인 처리
                if (existingUser == null) {
                    throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
                }
            }

            // 6. JWT 토큰 생성
            String accessToken = jwtService.generateToken(studentId);

            return new LoginResponse(HttpStatus.CREATED.value(), accessToken);

        } catch (IllegalArgumentException e) {
            // 잘못된 비밀번호 등의 경우
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        } catch (Exception e) {
            // 기타 오류 (학번이 존재하지 않는 경우 등)
            throw new RuntimeException("회원 정보를 찾을 수 없습니다.");
        }
    }

    @Transactional
    private void saveOrUpdateUser(String studentId, HansungDataResponse hansungData) {
        log.info("사용자 정보 저장/업데이트 시작: studentId={}", studentId);
        
        // 기존 사용자 조회
        Users existingUser = usersRepository.findByStudentId(studentId).orElse(null);
        
        if (existingUser != null) {
            log.info("기존 사용자 정보 업데이트: userId={}", existingUser.getId());
            // 기존 완료된 과목들 삭제 (새로 크롤링한 데이터로 업데이트)
            completedCourseRepository.deleteByUsers(existingUser);
            log.info("기존 완료된 과목들 삭제 완료: userId={}", existingUser.getId());
            // 기존 트랙 정보 삭제
            userTrackRepository.deleteByUsers(existingUser);
            log.info("기존 트랙 정보 삭제 완료: userId={}", existingUser.getId());
        } else {
            log.info("새 사용자 생성: studentId={}", studentId);
            existingUser = Users.builder()
                .studentId(studentId)
                .gpa(BigDecimal.ZERO)
                .build();
            existingUser = usersRepository.save(existingUser);
        }
        
        // 1. 사용자 트랙 정보 저장
        saveUserTracks(existingUser, hansungData.userInfo().tracks());
        
        // 2. GPA 계산 및 저장
        BigDecimal calculatedGpa = calculateGpa(hansungData.grades().semesters());
        existingUser.updateGpa(calculatedGpa);
        usersRepository.save(existingUser);
        log.info("GPA 계산 완료: studentId={}, gpa={}", studentId, calculatedGpa);
        
        // 3. 완료된 과목들 저장
        saveCompletedCourses(existingUser, hansungData.grades().semesters());

        System.out.println(hansungData.enrolledCourseNames());
        // 4. 수강 중인 과목들 저장
        saveEnrolledCourses(existingUser, hansungData.enrolledCourseNames());
        
        // 5. 사용자 lastCrawlTime과 updatedAt을 현재 시간으로 업데이트
        existingUser.updateLastCrawlTime(); // 크롤링 시간 업데이트
        existingUser = usersRepository.save(existingUser); // @LastModifiedDate 트리거
        log.info("사용자 크롤링 시간 업데이트 완료: studentId={}, lastCrawlTime={}, updatedAt={}", 
                studentId, existingUser.getLastCrawlTime(), existingUser.getUpdatedAt());
        
        log.info("사용자 정보 저장/업데이트 완료: studentId={}", studentId);
    }
    
    /**
     * 사용자 트랙 정보 저장
     */
    private void saveUserTracks(Users user, List<String> trackNames) {
        log.info("사용자 트랙 정보 저장 시작: userId={}, trackNames={}", user.getId(), trackNames);
        
        for (int i = 0; i < trackNames.size(); i++) {
            String trackName = trackNames.get(i);
            Optional<Track> trackOpt = trackRepository.findByTrackName(trackName);
            if (trackOpt.isPresent()) {
                Track track = trackOpt.get();
                
                // 첫번째 트랙은 PRIMARY, 두번째는 SECONDARY
                TrackType trackType = (i == 0) ? TrackType.PRIMARY : TrackType.SECONDARY;
                
                // 기존 데이터는 이미 삭제되었으므로 중복 체크 불필요
                
                UserTrack userTrack = UserTrack.builder()
                    .users(user)
                    .track(track)
                    .trackType(trackType)
                    .build();
                userTrackRepository.save(userTrack);
                log.info("트랙 정보 저장 완료: trackName={}, trackType={}", trackName, trackType);
            } else {
                log.warn("트랙을 찾을 수 없음: trackName={}", trackName);
            }
        }
    }
    
    /**
     * 모든 학기의 평점 평균을 계산하여 전체 GPA 계산
     */
    private BigDecimal calculateGpa(List<SemesterGradeResponse> semesters) {
        if (semesters == null || semesters.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalGpa = BigDecimal.ZERO;
        int semesterCount = 0;
        
        for (SemesterGradeResponse semester : semesters) {
            // 학기 요약에서 "평점평균" 찾기
            String gpaStr = semester.semesterSummary().get("평점평균");
            if (gpaStr != null && !gpaStr.trim().isEmpty()) {
                try {
                    BigDecimal semesterGpa = new BigDecimal(gpaStr.trim());
                    totalGpa = totalGpa.add(semesterGpa);
                    semesterCount++;
                    log.debug("학기 GPA 추가: semester={}, gpa={}", semester.semester(), semesterGpa);
                } catch (NumberFormatException e) {
                    log.warn("학기 GPA 파싱 실패: semester={}, gpaStr={}", semester.semester(), gpaStr);
                }
            }
        }
        
        if (semesterCount == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal averageGpa = totalGpa.divide(new BigDecimal(semesterCount), 2, RoundingMode.HALF_UP);
        log.info("전체 GPA 계산 완료: totalGpa={}, semesterCount={}, averageGpa={}", totalGpa, semesterCount, averageGpa);
        return averageGpa;
    }
    
    /**
     * 완료된 과목들 저장
     */
    private void saveCompletedCourses(Users user, List<SemesterGradeResponse> semesters) {
        log.info("완료된 과목 저장 시작: userId={}", user.getId());
        
        // 사용자의 트랙 정보 조회 (Primary, Secondary)
        List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
        Track primaryTrack = null;
        Track secondaryTrack = null;
        
        for (UserTrack userTrack : userTracks) {
            if (userTrack.getTrackType() == TrackType.PRIMARY) {
                primaryTrack = userTrack.getTrack();
            } else if (userTrack.getTrackType() == TrackType.SECONDARY) {
                secondaryTrack = userTrack.getTrack();
            }
        }
        
        log.info("사용자 트랙 정보: primaryTrack={}, secondaryTrack={}", 
                primaryTrack != null ? primaryTrack.getTrackName() : "null",
                secondaryTrack != null ? secondaryTrack.getTrackName() : "null");
        
        int savedCount = 0;
        int notFoundCount = 0;
        
        for (SemesterGradeResponse semester : semesters) {
            // 학기명에서 연도와 학기 추출 (예: "2024년 1학기" -> 2024, 1)
            int year = extractYearFromSemesterName(semester.semester());
            Semester semesterEnum = extractSemesterFromSemesterName(semester.semester());
            
            for (CourseGradeResponse course : semester.courses()) {
                // 과목 코드로 Course 엔티티 찾기
                Optional<Course> courseOpt = courseRepository.findByCourseCode(course.code());
                if (courseOpt.isPresent()) {
                    Course courseEntity = courseOpt.get();
                    
                    // 기존 데이터는 이미 삭제되었으므로 중복 체크 불필요
                    
                    // 성적을 CompletedGrade enum으로 변환
                    CompletedGrade completedGrade = convertToCompletedGrade(course.grade());
                    
                    // 과목의 trackStatus에 따라 사용자의 Primary/Secondary 트랙 매핑
                    Track track = mapTrackStatusToUserTrack(course.trackStatus(), primaryTrack, secondaryTrack);
                    
                    // CompletedCourse 엔티티 생성 및 저장
                    CompletedCourse completedCourse = CompletedCourse.builder()
                        .users(user)
                        .course(courseEntity)
                        .track(track) // 사용자의 Primary/Secondary 트랙 정보
                        .completedYear(year)
                        .gradeLevel(courseEntity.getOpenGrade()) // 과목의 개설학년 사용
                        .completedSemester(semesterEnum)
                        .completedGrade(completedGrade) // CompletedGrade enum 저장
                        .gradePoint(completedGrade.getGradePoint()) // 성적 평점 저장
                        .build();
                    
                    completedCourseRepository.save(completedCourse);
                    savedCount++;
                    log.debug("완료된 과목 저장: courseCode={}, grade={}, trackStatus={}, track={}", 
                            course.code(), course.grade(), course.trackStatus(), 
                            track != null ? track.getTrackName() : "null");
                } else {
                    notFoundCount++;
                    log.warn("과목을 찾을 수 없음: courseCode={}", course.code());
                }
            }
        }
        
        log.info("완료된 과목 저장 완료: userId={}, savedCount={}, notFoundCount={}", 
                user.getId(), savedCount, notFoundCount);
    }
    
    /**
     * 학기명에서 연도 추출 (예: "2025 학년도 1 학기" -> 2025)
     */
    private int extractYearFromSemesterName(String semesterName) {
        try {
            // "2025 학년도 1 학기" 형태에서 연도 추출
            String yearStr = semesterName.split(" 학년도")[0];
            return Integer.parseInt(yearStr.trim());
        } catch (Exception e) {
            log.warn("학기명에서 연도 추출 실패: semesterName={}", semesterName);
            return 2024; // 기본값
        }
    }
    
    /**
     * 학기명에서 Semester enum 추출 (예: "2025 학년도 1 학기" -> FIRST)
     */
    private Semester extractSemesterFromSemesterName(String semesterName) {
        if (semesterName.contains("1 학기")) {
            return Semester.FIRST;
        } else if (semesterName.contains("2 학기")) {
            return Semester.SECOND;
        } else {
            log.warn("학기명에서 학기 추출 실패: semesterName={}", semesterName);
            return Semester.FIRST; // 기본값
        }
    }
    
    /**
     * 성적 문자열을 CompletedGrade enum으로 변환
     */
    private CompletedGrade convertToCompletedGrade(String gradeStr) {
        if (gradeStr == null || gradeStr.trim().isEmpty()) {
            return CompletedGrade.F;
        }
        
        String trimmedGrade = gradeStr.trim();
        return switch (trimmedGrade) {
            case "A+" -> CompletedGrade.A_PLUS;
            case "A", "A0" -> CompletedGrade.A;
            case "B+" -> CompletedGrade.B_PLUS;
            case "B", "B0" -> CompletedGrade.B;
            case "C+" -> CompletedGrade.C_PLUS;
            case "C", "C0" -> CompletedGrade.C;
            case "D+" -> CompletedGrade.D_PLUS;
            case "D", "D0" -> CompletedGrade.D;
            case "F", "F0" -> CompletedGrade.F;
            case "P" -> CompletedGrade.PASS;
            default -> {
                log.warn("알 수 없는 성적: {}", trimmedGrade);
                yield CompletedGrade.F;
            }
        };
    }
    
    /**
     * 과목의 trackStatus를 사용자의 Primary/Secondary 트랙으로 매핑
     * "제1트랙" -> 사용자의 Primary 트랙
     * "제2트랙" -> 사용자의 Secondary 트랙
     * "" 또는 null -> null
     */
    private Track mapTrackStatusToUserTrack(String trackStatus, Track primaryTrack, Track secondaryTrack) {
        if (trackStatus == null || trackStatus.trim().isEmpty()) {
            return null;
        }

        // 이미 크롤링 서비스에서 정리된 트랙명을 받음
        // "제1트랙", "제2트랙", "" 등의 형태
        if ("제1트랙".equals(trackStatus)) {
            return primaryTrack; // 사용자의 Primary 트랙 반환
        } else if ("제2트랙".equals(trackStatus)) {
            return secondaryTrack; // 사용자의 Secondary 트랙 반환
        } else {
            // 빈 문자열이거나 알 수 없는 경우 null 반환
            return null;
        }
    }
    
    /**
     * 수강 중인 과목들 저장
     */
    private void saveEnrolledCourses(Users user, List<String> enrolledCourseNames) {
        if (enrolledCourseNames == null || enrolledCourseNames.isEmpty()) {
            log.info("수강 중인 과목이 없습니다: userId={}", user.getId());
            return;
        }
        
        log.info("수강 중인 과목 저장 시작: userId={}, enrolledCourseNames={}", user.getId(), enrolledCourseNames);
        
        // 기존 수강 과목 데이터 삭제
        enrolledCourseRepository.deleteByUser(user);
        
        int savedCount = 0;
        int notFoundCount = 0;
        
        for (String courseName : enrolledCourseNames) {
            var courseOpt = courseRepository.findByCourseName(courseName);
            if (courseOpt.isPresent()) {
                Course course = courseOpt.get();
                EnrolledCourse enrolledCourse = EnrolledCourse.builder()
                        .user(user)
                        .course(course)
                        .build();
                enrolledCourseRepository.save(enrolledCourse);
                savedCount++;
                log.info("수강 과목 저장 완료: {} - {}", courseName, course.getCourseCode());
            } else {
                notFoundCount++;
                log.warn("수강 과목을 DB에서 찾을 수 없음: {}", courseName);
            }
        }
        
        log.info("수강 과목 저장 완료 - 저장됨: {}, 찾을 수 없음: {}, 전체: {}", 
                savedCount, notFoundCount, enrolledCourseNames.size());
    }
}

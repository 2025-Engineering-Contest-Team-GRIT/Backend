package grit.guidance.domain.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.user.dto.*;
import grit.guidance.domain.user.entity.UserTrack;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.domain.user.repository.UserTrackRepository;
import grit.guidance.domain.user.repository.UsersRepository;
import grit.guidance.domain.roadmap.entity.RecommendedCourse;
import grit.guidance.domain.roadmap.repository.RecommendedCourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UsersRepository usersRepository;
    private final UserTrackRepository userTrackRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final ObjectMapper objectMapper;

    public DashboardResponseDto getDashboardData(String studentId) {
        try {
            // 1. 사용자 정보 조회
            Users user = usersRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 2. 사용자 트랙 정보 조회
            List<UserTrack> userTracks = userTrackRepository.findByUsers(user);
            List<String> trackNames = userTracks.stream()
                    .map(ut -> ut.getTrack().getTrackName())
                    .toList();

            // 3. 사용자 정보 DTO 생성
            UserInfoDto userInfo = new UserInfoDto(
                    user.getName(),
                    user.getGrade(),
                    user.getSemester().ordinal() + 1, // FIRST=1, SECOND=2
                    "컴퓨터공학부", // 하드코딩 또는 사용자 테이블에서 가져오기
                    trackNames
            );

            // 4. 학업 현황 DTO 생성
            AcademicStatusDto academicStatus = new AcademicStatusDto(
                    user.getGpa(),
                    new BigDecimal("4.5"), // 기본값
                    user.getEarnedCredits(),
                    130 // 기본값
            );

            // 5. 진로 목표 DTO 생성 (주 트랙들)
            String primaryTracks = String.join(" / ", trackNames);
            CareerGoalDto careerGoal = new CareerGoalDto(primaryTracks);

            // 6. 다음 학기 추천 과목 조회
            List<NextSemesterCourseDto> nextSemesterCourses = getNextSemesterCourses(user);

            // 7. 오늘의 시간표 조회
            List<TimetableDetailDto> todaySchedule = getTodaySchedule(user);

            // 8. 대시보드 데이터 생성
            DashboardDataDto dashboardData = new DashboardDataDto(
                    userInfo,
                    academicStatus,
                    careerGoal,
                    nextSemesterCourses,
                    todaySchedule
            );

            return DashboardResponseDto.success(dashboardData);

        } catch (Exception e) {
            log.error("대시보드 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            return DashboardResponseDto.serverError();
        }
    }

    /**
     * 다음 학기 추천 과목 조회
     */
    private List<NextSemesterCourseDto> getNextSemesterCourses(Users user) {
        try {
            // 다음 학기 계산
            Integer nextGrade = user.getGrade() + 1;
            Semester nextSemester = user.getSemester() == Semester.FIRST ? Semester.SECOND : Semester.FIRST;
            
            // 다음 학기에 해당하는 추천 과목만 조회
            List<RecommendedCourse> recommendedCourses = recommendedCourseRepository
                    .findByUserAndRecommendGradeAndRecommendSemester(user, nextGrade, nextSemester);
            
            return recommendedCourses.stream()
                    .map(rc -> new NextSemesterCourseDto(
                            rc.getCourse().getId(),
                            rc.getCourse().getCourseName()
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("다음 학기 추천 과목 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 오늘의 시간표 조회
     */
    private List<TimetableDetailDto> getTodaySchedule(Users user) {
        try {
            if (user.getTimetable() == null || user.getTimetable().trim().isEmpty()) {
                return new ArrayList<>();
            }

            // JSON 문자열을 TimetableDetailDto 리스트로 파싱
            List<TimetableDetailDto> allSchedules = objectMapper.readValue(
                    user.getTimetable(),
                    new TypeReference<List<TimetableDetailDto>>() {}
            );

            // 오늘의 요일 계산
            String todayDay = getTodayDayOfWeek();

            // 오늘 요일에 해당하는 강의만 필터링
            return allSchedules.stream()
                    .filter(schedule -> todayDay.equals(schedule.day()))
                    .toList();

        } catch (Exception e) {
            log.warn("오늘의 시간표 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 오늘의 요일을 한글로 반환
     */
    private String getTodayDayOfWeek() {
        LocalDate today = LocalDate.now();
        return switch (today.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }
}

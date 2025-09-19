package grit.guidance.domain.simulation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import grit.guidance.domain.user.dto.UserTrackDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class SimulationDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationDataResponseDto {
        private GraduationResponseDto crawlingData;
        private List<AvailableCourseDto> availableCourses;
        private List<UserTrackDto> userTracks;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AvailableCourseDto {
        private String courseCode;
        private String courseName;
        private Integer credit;
        private String courseType;

        // [수정] 여기에 필드들이 다시 추가되어야 합니다.
        private Integer openGrade;
        private Semester openSemester;
        private List<Long> applicableTrackIds;
    }

    // ... (PlanSummaryDto, PlanDetailDto, CourseDetailDto 등 나머지 DTO는 그대로 유지)
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSummaryDto {
        private Long planId;
        private String planName;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanDetailDto {
        private Long planId;
        private String planName;
        private LocalDateTime createdAt;
        private List<CourseDetailDto> selectedCourses;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDetailDto {
        private String courseCode;
        private String courseName;
        private Integer credits;
        // 상세 조회 DTO에도 이 정보가 있는 것은 좋습니다!
        private Integer openGrade;
        private Semester openSemester;
    }
}
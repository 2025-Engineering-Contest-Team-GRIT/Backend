package grit.guidance.domain.simulation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class SimulationDto {

    // ... (기존 DTO들은 그대로 유지)
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulationDataResponseDto {
        private GraduationResponseDto crawlingData;
        private List<AvailableCourseDto> availableCourses;
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
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSummaryDto {
        private Long planId;
        private String planName;
        private LocalDateTime createdAt;
    }

    // ==========================================================
    // == ▼▼▼ 상세 계획 조회를 위해 새로 추가된 DTO ▼▼▼ ==
    // ==========================================================

    /**
     * 특정 졸업 계획의 상세 정보를 담는 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanDetailDto {
        private Long planId;
        private String planName;
        private LocalDateTime createdAt;
        private List<CourseDetailDto> selectedCourses; // 계획에 포함된 과목 목록
    }

    /**
     * 상세 계획에 포함된 개별 과목의 정보를 담는 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDetailDto {
        private String courseCode;
        private String courseName;
        private Integer credits;
    }
}


package grit.guidance.domain.simulation.dto;

import grit.guidance.domain.graduation.dto.GraduationResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class SimulationDto {

    // 시뮬레이션 페이지 전체 데이터를 담는 DTO
    @Getter
    @Builder
    public static class SimulationDataResponseDto {
        private GraduationResponseDto crawlingData; // 크롤링으로 가져온 현재 이수 현황
        private List<AvailableCourseDto> availableCourses; // 수강 가능한 과목 목록
    }

    // 수강 가능한 개별 과목 정보를 담는 DTO
    @Getter
    @Builder
    public static class AvailableCourseDto {
        private String courseCode; // 과목 코드 (예: V020021)
        private String courseName; // 과목명 (예: 고급시스템프로그래밍)
        private Integer credit;      // 학점
        private String courseType;   // "MANDATORY" 또는 "ELECTIVE"
    }

}

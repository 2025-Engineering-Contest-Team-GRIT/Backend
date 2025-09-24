package grit.guidance.domain.simulation.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class GraduationPlanRequestDto {
    private String studentId;
    private String planName;
    private List<SelectedCourse> selectedCourses; // 시뮬레이션에서 선택된 과목과 트랙 정보 목록

    @Getter
    public static class SelectedCourse {
        private String courseCode;
        private Long trackId;
    }
}

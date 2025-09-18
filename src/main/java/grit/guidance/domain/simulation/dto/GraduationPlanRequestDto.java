package grit.guidance.domain.simulation.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class GraduationPlanRequestDto {
    private String studentId;
    private String planName;
    private List<String> selectedCourseCodes; // 시뮬레이션에서 선택된 과목 코드 목록
}

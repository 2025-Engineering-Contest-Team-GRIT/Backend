package grit.guidance.domain.user.dto;

import java.util.List;
import java.util.Map;

// 학기별 전체 성적 정보를 담는 DTO
public record SemesterGradeResponse(String semester, Map<String, String> semesterSummary, List<CourseGradeResponse> courses) {}

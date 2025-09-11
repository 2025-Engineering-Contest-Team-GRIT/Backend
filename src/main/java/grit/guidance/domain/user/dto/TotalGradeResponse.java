package grit.guidance.domain.user.dto;

import java.util.List;
import java.util.Map;

// 전체 성적 정보를 담는 DTO
public record TotalGradeResponse(Map<String, String> creditSummary, List<SemesterGradeResponse> semesters) {}

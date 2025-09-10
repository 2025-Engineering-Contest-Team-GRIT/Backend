package grit.guidance.domain.user.dto;

// 학기별 과목 정보를 담는 DTO
public record CourseGradeResponse(String classification, String name, String code, String credits, String grade) {}
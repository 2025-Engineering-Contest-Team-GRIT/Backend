package grit.guidance.domain.roadmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CourseDto(
    @JsonProperty("courseId")
    Long courseId,

    @JsonProperty("courseName")
    String courseName,

    @JsonProperty("credits")
    Integer credits,

    @JsonProperty("status")
    String status, // "COMPLETED", "RECOMMENDED", "TAKING"

    @JsonProperty("courseType")
    String courseType,

    @JsonProperty("prerequisiteIds")
    List<Long> prerequisiteIds,

    @JsonProperty("course_description")
    String courseDescription,

    // 이수 완료 과목에만 포함
    @JsonProperty("completed_grade")
    String completedGrade,

    // 추천 과목에만 포함
    @JsonProperty("recommended_description")
    String recommendedDescription
) {
    public static CourseDto createCompleted(Long courseId, String courseName, Integer credits, 
                                          String courseType, List<Long> prerequisiteIds, String courseDescription, String completedGrade) {
        return new CourseDto(courseId, courseName, credits, "COMPLETED", courseType, prerequisiteIds, 
                           courseDescription, completedGrade, null);
    }

    public static CourseDto createTaking(Long courseId, String courseName, Integer credits, 
                                       String courseType, List<Long> prerequisiteIds, String courseDescription) {
        return new CourseDto(courseId, courseName, credits, "TAKING", courseType, prerequisiteIds, 
                           courseDescription, null, null);
    }

    public static CourseDto createRecommended(Long courseId, String courseName, Integer credits, 
                                            String courseType, List<Long> prerequisiteIds, String courseDescription, String recommendedDescription) {
        return new CourseDto(courseId, courseName, credits, "RECOMMENDED", courseType, prerequisiteIds, 
                           courseDescription, null, recommendedDescription);
    }
}

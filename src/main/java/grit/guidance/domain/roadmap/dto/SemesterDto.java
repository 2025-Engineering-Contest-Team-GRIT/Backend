package grit.guidance.domain.roadmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SemesterDto(
    @JsonProperty("year")
    Integer year,

    @JsonProperty("semester")
    Integer semester,

    @JsonProperty("totalCredits")
    Integer totalCredits,

    @JsonProperty("courses")
    List<CourseDto> courses
) {}

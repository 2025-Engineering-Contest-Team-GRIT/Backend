package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NextSemesterCourseDto(
    @JsonProperty("courseId")
    Long courseId,
    
    @JsonProperty("courseName")
    String courseName
) {}

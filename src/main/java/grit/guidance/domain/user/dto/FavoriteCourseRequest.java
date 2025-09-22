package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FavoriteCourseRequest(
    @JsonProperty("courseId")
    Long courseId
) {}

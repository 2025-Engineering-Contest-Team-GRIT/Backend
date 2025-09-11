package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
    @JsonProperty("student_id")
    String studentId,
    String password
) {
}

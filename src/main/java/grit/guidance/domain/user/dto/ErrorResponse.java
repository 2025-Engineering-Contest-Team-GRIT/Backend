package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorResponse(
    @JsonProperty("status")
    Integer status,
    @JsonProperty("message")
    String message
) {
}

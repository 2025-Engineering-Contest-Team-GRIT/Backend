package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
    @JsonProperty("status")
    Integer status,
    @JsonProperty("access_token")
    String accessToken
) {
}

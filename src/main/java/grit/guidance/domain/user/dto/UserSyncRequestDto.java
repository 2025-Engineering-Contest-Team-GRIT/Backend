package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserSyncRequestDto(
        @JsonProperty("studentId") String studentId, // <-- 명시적 선언
        @JsonProperty("password") String password
) {}
package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CareerGoalDto(
    @JsonProperty("primaryTracks")
    String primaryTracks
) {}

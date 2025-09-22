package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UserInfoDto(
    @JsonProperty("name")
    String name,
    
    @JsonProperty("year")
    Integer year,
    
    @JsonProperty("semester")
    Integer semester,
    
    @JsonProperty("department")
    String department,
    
    @JsonProperty("tracks")
    List<String> tracks
) {}

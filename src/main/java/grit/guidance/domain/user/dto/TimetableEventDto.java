package grit.guidance.domain.user.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TimetableEventDto(
    @JsonProperty("title")
    String title,
    
    @JsonProperty("start")
    String start,
    
    @JsonProperty("end")
    String end
) {}
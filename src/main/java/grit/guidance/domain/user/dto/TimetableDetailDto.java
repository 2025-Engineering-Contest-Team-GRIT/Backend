package grit.guidance.domain.user.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TimetableDetailDto(
    @JsonProperty("courseName")
    String courseName,
    
    @JsonProperty("professorName")
    String professorName,
    
    @JsonProperty("classroom")
    String classroom,
    
    @JsonProperty("day")
    String day,
    
    @JsonProperty("start")
    String start,
    
    @JsonProperty("end")
    String end
) {}

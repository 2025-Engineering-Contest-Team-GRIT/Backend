package grit.guidance.domain.roadmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RoadmapDataDto(
    @JsonProperty("semesters")
    List<SemesterDto> semesters,
    
    @JsonProperty("currentGrade")
    Integer currentGrade,
    
    @JsonProperty("currentSemester")
    String currentSemester
) {}

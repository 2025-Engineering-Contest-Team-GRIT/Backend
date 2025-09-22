package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TimetableDataDto(
    @JsonProperty("timetable")
    List<TimetableDetailDto> timetable
) {}

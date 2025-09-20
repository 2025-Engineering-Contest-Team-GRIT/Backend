package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DashboardDataDto(
    @JsonProperty("userInfo")
    UserInfoDto userInfo,
    
    @JsonProperty("academicStatus")
    AcademicStatusDto academicStatus,
    
    @JsonProperty("careerGoal")
    CareerGoalDto careerGoal,
    
    @JsonProperty("nextSemesterCourses")
    List<NextSemesterCourseDto> nextSemesterCourses,
    
    @JsonProperty("todaySchedule")
    List<TimetableDetailDto> todaySchedule
) {}

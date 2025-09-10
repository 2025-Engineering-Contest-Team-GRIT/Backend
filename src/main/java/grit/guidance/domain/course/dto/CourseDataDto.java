package grit.guidance.domain.course.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseDataDto {

    private int year;
    private String semester;
    private String category;
    private String id;
    private String name;
    private int credits;
    private String track;

}
package grit.guidance.domain.user.dto;

import grit.guidance.domain.course.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCourseDto {
    private Course course;
    private String status; // "completed", "enrolled", "available"
    private String courseType; // "전공필수", "전공선택", "전공기초", "일반선택"
    private Long trackId; // completed_course에 있을 때만 반환, 전공필수일 때만 반환
    private Integer grade; // 학년
    private String semester; // 학기
    private Boolean isFavorite; // 관심과목 여부
}

package grit.guidance.domain.graduation.dto;

import grit.guidance.domain.user.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawlingGraduationDto {
    private Users users;
    private Integer totalCompletedCredits;
    private Integer track1MajorBasic;
    private Integer track1MajorRequired;
    private Integer track1MajorSubtotal;
    private Integer track2MajorBasic;
    private Integer track2MajorRequired;
    private Integer track2MajorSubtotal;
    private Integer totalMajorCompleted;
    private Integer totalMajorRequired;
}
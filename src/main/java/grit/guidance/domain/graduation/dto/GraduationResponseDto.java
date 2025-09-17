package grit.guidance.domain.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationResponseDto {
    private Integer totalCompletedCredits;
    private Integer totalRequiredCredits;
    private List<TrackProgressDto> trackProgressList;
    private List<CertificationStatusDto> certifications;
}
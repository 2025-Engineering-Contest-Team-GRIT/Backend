package grit.guidance.domain.graduation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardResponseDto {
    private int totalCompletedCredits;
    private int totalRequiredCredits;
    private List<TrackProgressDto> trackProgressList;
    private List<CertificationStatusDto> certifications;
}
package grit.guidance.domain.graduation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardResponseDto {
    private int totalCompletedCredits; // 총 이수 학점
    private int totalRequiredCredits;  // 총 필요 학점
    private List<TrackProgressDto> trackProgressList; // 트랙별 진척도
    private List<CertificationStatusDto> certifications; // 졸업 인증 상태
}
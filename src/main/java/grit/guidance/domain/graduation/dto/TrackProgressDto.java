package grit.guidance.domain.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TrackProgressDto {
    private String trackName;
    private String category;       // category 필드 추가
    private int requiredCredits;
    private int completedCredits;
    private int remainingCredits;
    private double progressRate;   // 퍼센트
}

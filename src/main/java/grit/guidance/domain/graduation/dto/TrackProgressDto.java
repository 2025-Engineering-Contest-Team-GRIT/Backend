package grit.guidance.domain.graduation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackProgressDto {
    private String trackName;           // 트랙 이름
    private int completedCredits;      // 이수한 학점
    private int requiredCredits;       // 필요 학점
    private String category;            // 전공기초, 전공필수 등
}
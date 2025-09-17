package grit.guidance.domain.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackProgressDto {
    private String trackName;
    private String category;
    private DetailedCreditDto majorBasic;
    private DetailedCreditDto majorRequired;
    private DetailedCreditDto majorSubtotal; // 기존 필드 대신 상세 DTO로 변경
}
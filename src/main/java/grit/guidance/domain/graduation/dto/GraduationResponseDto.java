package grit.guidance.domain.graduation.dto;

import grit.guidance.domain.graduation.entity.CrawlingGraduation;
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

    // 이 메소드를 추가하여 GraduationService에서 호출할 수 있게 합니다.
    public static GraduationResponseDto from(CrawlingGraduation crawlingData, List<TrackProgressDto> trackProgressList, List<CertificationStatusDto> certifications) {
        return GraduationResponseDto.builder()
                .totalCompletedCredits(crawlingData.getTotalCompletedCredits())
                .totalRequiredCredits(crawlingData.getTotalMajorRequired())
                .trackProgressList(trackProgressList)
                .certifications(certifications)
                .build();
    }
}
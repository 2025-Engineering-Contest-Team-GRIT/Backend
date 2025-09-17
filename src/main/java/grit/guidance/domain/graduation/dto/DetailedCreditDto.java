package grit.guidance.domain.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedCreditDto {
    private Integer completedCredits;
    private Integer requiredCredits;
}
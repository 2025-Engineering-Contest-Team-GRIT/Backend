package grit.guidance.domain.graduation.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedCreditDto {
    private int completedCredits;
    private int requiredCredits;
    private int remainingCredits;
    private double progressRate;
}
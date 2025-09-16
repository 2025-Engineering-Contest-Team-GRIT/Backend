package grit.guidance.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MajorRequiredCreditsResponse {
    private MajorCreditDetail track1;
    private MajorCreditDetail track2;
    private MajorCreditTotal total;
}
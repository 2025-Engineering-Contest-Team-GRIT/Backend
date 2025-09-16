package grit.guidance.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MajorCreditDetail {
    private String majorBasic;
    private String majorRequired;
    private String majorSubtotal;
}
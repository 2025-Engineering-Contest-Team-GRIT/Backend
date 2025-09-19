package grit.guidance.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 'public static class'에서 'static'을 제거합니다.
public class UserTrackDto {
    private Long trackId;
    private String trackName;
}
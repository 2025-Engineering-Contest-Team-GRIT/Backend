package grit.guidance.domain.roadmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoadmapResponseDto(
    @JsonProperty("status")
    Integer status,

    @JsonProperty("message")
    String message,

    @JsonProperty("data")
    RoadmapDataDto data
) {
    public static RoadmapResponseDto success(RoadmapDataDto data) {
        return new RoadmapResponseDto(200, "사용자의 전체 로드맵 정보를 성공적으로 조회했습니다.", data);
    }

    public static RoadmapResponseDto unauthorized() {
        return new RoadmapResponseDto(401, "로그인이 필요합니다.", null);
    }

    public static RoadmapResponseDto serverError() {
        return new RoadmapResponseDto(500, "서버 내부 오류가 발생했습니다.", null);
    }
}

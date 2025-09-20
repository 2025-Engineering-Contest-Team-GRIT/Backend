package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DashboardResponseDto(
    @JsonProperty("status")
    Integer status,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("data")
    DashboardDataDto data
) {
    public static DashboardResponseDto success(DashboardDataDto data) {
        return new DashboardResponseDto(200, "대시보드 정보를 성공적으로 조회했습니다.", data);
    }
    
    public static DashboardResponseDto unauthorized() {
        return new DashboardResponseDto(401, "로그인이 필요합니다.", null);
    }
    
    public static DashboardResponseDto serverError() {
        return new DashboardResponseDto(500, "서버 내부 오류가 발생했습니다.", null);
    }
}

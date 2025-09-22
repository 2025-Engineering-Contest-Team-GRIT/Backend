package grit.guidance.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record TimetableResponse(
    @JsonProperty("status")
    Integer status,

    @JsonProperty("message")
    String message,

    @JsonProperty("data")
    TimetableDataDto data
) {
    public static TimetableResponse success(List<TimetableDetailDto> timetable) {
        return new TimetableResponse(200, "시간표를 성공적으로 조회했습니다.", 
                new TimetableDataDto(timetable));
    }

    public static TimetableResponse unauthorized() {
        return new TimetableResponse(401, "로그인이 필요합니다.", null);
    }

    public static TimetableResponse serverError() {
        return new TimetableResponse(500, "서버 내부 오류가 발생했습니다.", null);
    }
}

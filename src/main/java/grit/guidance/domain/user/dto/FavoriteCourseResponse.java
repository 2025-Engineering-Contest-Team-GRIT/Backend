package grit.guidance.domain.user.dto;

public record FavoriteCourseResponse(
    Integer status,
    String message
) {
    public static FavoriteCourseResponse success() {
        return new FavoriteCourseResponse(200, "관심과목이 성공적으로 추가되었습니다.");
    }
    
    public static FavoriteCourseResponse alreadyExists() {
        return new FavoriteCourseResponse(409, "이미 관심과목으로 등록된 과목입니다.");
    }
    
    public static FavoriteCourseResponse notFound() {
        return new FavoriteCourseResponse(404, "과목을 찾을 수 없습니다.");
    }
    
    public static FavoriteCourseResponse unauthorized() {
        return new FavoriteCourseResponse(401, "로그인이 필요합니다.");
    }
    
    public static FavoriteCourseResponse serverError() {
        return new FavoriteCourseResponse(500, "서버 내부 오류가 발생했습니다.");
    }
}

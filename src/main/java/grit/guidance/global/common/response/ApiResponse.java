package grit.guidance.global.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final boolean isSuccess;
    private final String code;
    private final String message;
    private final T result;

    // 성공 시 사용하는 팩토리 메소드
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, "200", "요청 성공", result);
    }

    // 성공 시 메시지를 커스텀하는 경우
    public static <T> ApiResponse<T> onSuccess(String message, T result) {
        return new ApiResponse<>(true, "200", message, result);
    }

    // 실패 시 사용하는 팩토리 메소드
    public static <T> ApiResponse<T> onFailure(String code, String message, T result) {
        return new ApiResponse<>(false, code, message, result);
    }

    // 문제가 되는 ok() 메소드를 삭제했습니다.
}

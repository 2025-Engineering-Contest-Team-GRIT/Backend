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

    // 실패 시 사용하는 팩토리 메소드 (커스텀 에러 코드를 포함)
    public static <T> ApiResponse<T> onFailure(String code, String message, T result) {
        return new ApiResponse<>(false, code, message, result);
    }
}

//응답 통일: API 응답의 형식을 표준화합니다. 예를 들어, 모든 성공 응답은 {"isSuccess": true, "code": "200", "message": "요청 성공", "result": ...}와 같은 구조를 갖도록 만듭니다.
//
//응답 메시지: API 호출의 성공/실패 여부, 상태 코드, 그리고 개발자나 클라이언트가 이해하기 쉬운 메시지를 포함시킬 수 있습니다.
//
//유연성: 데이터를 감싸서 보내므로, 데이터 외에 다른 정보(예: 오류 메시지, 검증 오류 등)를 추가로 전달할 수 있어 유연하게 대응할 수 있습니다.
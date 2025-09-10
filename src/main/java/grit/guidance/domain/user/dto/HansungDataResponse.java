package grit.guidance.domain.user.dto;

// 최종 크롤링 결과를 담는 DTO
public record HansungDataResponse(UserInfoResponse userInfo, TotalGradeResponse grades) {}
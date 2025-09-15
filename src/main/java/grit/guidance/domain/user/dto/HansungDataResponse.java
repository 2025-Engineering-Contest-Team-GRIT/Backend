package grit.guidance.domain.user.dto;

import java.util.List;

// 최종 크롤링 결과를 담는 DTO
public record HansungDataResponse(UserInfoResponse userInfo, TotalGradeResponse grades, List<String> enrolledCourseNames) {}
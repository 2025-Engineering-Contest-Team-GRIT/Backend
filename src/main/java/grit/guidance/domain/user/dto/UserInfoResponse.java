package grit.guidance.domain.user.dto;

import java.util.List;

// UserInfo를 담는 DTO
public record UserInfoResponse(String name, List<String> tracks) {}
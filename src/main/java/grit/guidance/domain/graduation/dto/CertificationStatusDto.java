package grit.guidance.domain.graduation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertificationStatusDto {
    private String certificationName; // 인증 항목 이름 (예: "캡스톤 디자인 발표회 작품 출품")
    private boolean isCompleted;      // 완료 여부 (true: 완료, false: 미완료)
}
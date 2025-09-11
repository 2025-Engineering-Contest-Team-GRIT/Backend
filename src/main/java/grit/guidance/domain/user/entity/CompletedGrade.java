package grit.guidance.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum CompletedGrade {
    A_PLUS("A+", new BigDecimal("4.5")),
    A("A", new BigDecimal("4.0")),
    B_PLUS("B+", new BigDecimal("3.5")),
    B("B", new BigDecimal("3.0")),
    C_PLUS("C+", new BigDecimal("2.5")),
    C("C", new BigDecimal("2.0")),
    D_PLUS("D+", new BigDecimal("1.5")),
    D("D", new BigDecimal("1.0")),
    F("F", new BigDecimal("0.0")),
    PASS("P", null), // P/F 과목은 평점 계산에서 제외되므로 null
    FAIL("FAIL", null); // F와 구분하기 위해 FAIL로 변경

    private final String description;
    private final BigDecimal gradePoint;
}
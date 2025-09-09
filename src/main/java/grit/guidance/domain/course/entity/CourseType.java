package grit.guidance.domain.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseType {
    MANDATORY("전공필수"),
    ELECTIVE("전공선택"),
    FOUNDATION("전공기초"),
    GENERAL_ELECTIVE("일반선택")
    ;

    private final String description;
}
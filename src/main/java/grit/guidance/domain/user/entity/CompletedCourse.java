package grit.guidance.domain.user.entity;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "completed_course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE completed_course SET updated_at = NOW(), deleted_at = NOW() WHERE completed_course_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CompletedCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "completed_course_id")
    private Long id;

    // N:1 관계 - completed_course와 users (양방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    // N:1 관계 - completed_course와 course (단방향, course 도메인 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "completed_year", nullable = false)
    private Integer completedYear; // 이수년도

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel; // 이수학년 (1-4, 추가학년은 4)

    @Enumerated(EnumType.STRING)
    @Column(name = "completed_semester", nullable = false)
    private Semester completedSemester; // 이수학기 (1, 2, 여름학기, 계절학기)

    @Enumerated(EnumType.STRING)
    @Column(name = "completed_grade", nullable = false)
    private CompletedGrade completedGrade; // 성적 등급 (A+, P 등)

    @Column(name = "grade_point", precision = 2, scale = 1)
    private BigDecimal gradePoint; // 성적 평점 (4.5, 4.0 등)

    @Builder
    public CompletedCourse(Users users, Course course, Integer completedYear, 
                          Integer gradeLevel, Semester completedSemester, CompletedGrade completedGrade, BigDecimal gradePoint) {
        this.users = users;
        this.course = course;
        this.completedYear = completedYear;
        this.gradeLevel = gradeLevel;
        this.completedSemester = completedSemester;
        this.completedGrade = completedGrade;
        this.gradePoint = gradePoint;
    }

    // 비즈니스 메서드
    public void updateCompletedInfo(Integer completedYear, Integer gradeLevel, Semester completedSemester, CompletedGrade completedGrade) {
        validateGradeLevel(gradeLevel);
        this.completedYear = completedYear;
        this.gradeLevel = gradeLevel; // 이수학년
        this.completedSemester = completedSemester;
        this.completedGrade = completedGrade; // 이수 성적
        this.gradePoint = completedGrade.getGradePoint();
    }

    private void validateGradeLevel(Integer grade) {
        if (grade < 1 || grade > 4) {
            throw new IllegalArgumentException("이수학년은 1-4 범위여야 합니다.");
        }
    }
}


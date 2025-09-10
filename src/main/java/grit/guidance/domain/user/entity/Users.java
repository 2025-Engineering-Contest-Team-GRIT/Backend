package grit.guidance.domain.user.entity;

import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET updated_at = NOW(), deleted_at = NOW() WHERE user_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Users extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true, length = 10)
    private String studentId; // 7자리 학번

    @Column(name = "GPA", nullable = false, precision = 3, scale = 2)
    private BigDecimal gpa = BigDecimal.ZERO; // 0.0-4.5 범위

    @Column(name = "timetable", columnDefinition = "TEXT")
    private String timetable; // JSON 형태의 시간표

    // 1:1 관계 - users와 setting (양방향)
    @OneToOne(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Setting setting;

    // 1:1 관계 - users와 graduation_requirement (양방향)
    @OneToOne(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GraduationRequirement graduationRequirement;

    // 1:N 관계 - users와 completed_course (양방향)
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CompletedCourse> completedCourses = new ArrayList<>();

    // 1:N 관계 - users와 favorite_course (양방향)
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FavoriteCourse> favoriteCourses = new ArrayList<>();

    @Builder
    public Users(String studentId, BigDecimal gpa, String timetable) {
        this.studentId = studentId;
        this.gpa = gpa != null ? gpa : BigDecimal.ZERO;
        this.timetable = timetable;
    }

    // 비즈니스 메서드
    public void updateGpa(BigDecimal newGpa) {
        if (newGpa.compareTo(BigDecimal.ZERO) < 0 || newGpa.compareTo(new BigDecimal("4.5")) > 0) {
            throw new IllegalArgumentException("GPA는 0.0-4.5 범위여야 합니다.");
        }
        this.gpa = newGpa;
    }

    public void updateTimetable(String timetable) {
        this.timetable = timetable;
    }
}


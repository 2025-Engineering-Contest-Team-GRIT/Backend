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

@Entity
@Table(name = "enrolled_course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE enrolled_course SET updated_at = NOW(), deleted_at = NOW() WHERE enrolled_course_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class EnrolledCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrolled_course_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "enrolled_year", nullable = false)
    private Integer enrolledYear; // 수강 년도

    @Enumerated(EnumType.STRING)
    @Column(name = "enrolled_semester", nullable = false)
    private Semester enrolledSemester; // 수강 학기

    @Builder
    private EnrolledCourse(Users user, Course course, Integer enrolledYear, Semester enrolledSemester) {
        this.user = user;
        this.course = course;
        this.enrolledYear = enrolledYear;
        this.enrolledSemester = enrolledSemester;
    }
}
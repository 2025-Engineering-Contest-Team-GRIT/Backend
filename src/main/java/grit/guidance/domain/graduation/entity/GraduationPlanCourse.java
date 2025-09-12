package grit.guidance.domain.graduation.entity;


import grit.guidance.domain.course.entity.Course;
import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "graduation_plan_course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE graduation_plan_course SET updated_at = NOW(), deleted_at = NOW() WHERE graduation_plan_course_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GraduationPlanCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "graduation_plan_course_id")
    private Long id;

    // 다대일 관계 - graduation_plan_course와 graduation_plan (단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graduation_plan_id", nullable = false)
    private GraduationPlan graduationPlan;

    // 다대일 관계 - graduation_plan_course와 course (단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Builder
    private GraduationPlanCourse(GraduationPlan graduationPlan, Course course) {
        this.graduationPlan = graduationPlan;
        this.course = course;
    }
}
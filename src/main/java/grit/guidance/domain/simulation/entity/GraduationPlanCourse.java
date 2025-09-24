package grit.guidance.domain.simulation.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graduation_plan_id", nullable = false)
    private GraduationPlan graduationPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "track")
    private Long track;

    @Builder
    private GraduationPlanCourse(GraduationPlan graduationPlan, Course course, Long track) {
        this.graduationPlan = graduationPlan;
        this.course = course;
        this.track = track;
    }
}

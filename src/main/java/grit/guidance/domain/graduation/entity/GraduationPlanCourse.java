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
    @Column(name = "graduation_plan_course_id") //졸업시뮬레이션 할 때 사용될 과목의 id
    private Long id;

    // 다대일 관계 - graduation_plan_course와 graduation_plan (단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graduation_plan_id", nullable = false)
    private GraduationPlan graduationPlan; //졸업계획 id 와 연결

    // 다대일 관계 - graduation_plan_course와 course (단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course; //course에서 id를 가져올 예정

    @Builder
    private GraduationPlanCourse(GraduationPlan graduationPlan, Course course) {
        this.graduationPlan = graduationPlan;
        this.course = course;
    }
}
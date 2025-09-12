package grit.guidance.domain.graduation.entity;

import grit.guidance.domain.user.entity.Users;
import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import grit.guidance.domain.graduation.entity.GraduationPlanCourse;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "graduation_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE graduation_plan SET updated_at = NOW(), deleted_at = NOW() WHERE graduation_plan_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GraduationPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "graduation_plan_id")
    private Long id;

    @Column(name = "plan_name", nullable = false, length = 30)
    private String planName;

    // 다대일 관계 - graduation_plan과 user (단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    // 일대다 관계 - graduation_plan과 graduation_plan_course (양방향)
    @OneToMany(mappedBy = "graduationPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GraduationPlanCourse> graduationPlanCourses = new ArrayList<>();

    @Builder
    private GraduationPlan(String planName, Users users) {
        this.planName = planName;
        this.users = users;
    }
}
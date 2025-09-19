package grit.guidance.domain.simulation.entity;

import grit.guidance.domain.user.entity.Users;
import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    // GraduationPlan이 삭제되면 관련된 GraduationPlanCourse도 함께 삭제되도록 설정합니다.
    @OneToMany(mappedBy = "graduationPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GraduationPlanCourse> graduationPlanCourses = new ArrayList<>();

    @Builder
    private GraduationPlan(String planName, Users users) {
        this.planName = planName;
        this.users = users;
    }

    /**
     * 계획 이름을 수정하는 헬퍼(도우미) 메소드
     */
    public void updatePlanName(String planName) {
        this.planName = planName;
    }
}


package grit.guidance.domain.course.entity;

import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "course_prerequisite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE course_prerequisite SET updated_at = NOW(), deleted_at = NOW() WHERE course_prerequisite_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CoursePrerequisite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_prerequisite_id")
    private Long id;

    // 1:N 관계 - course와 course_prerequisite (과목이 여러 선수과목을 가질 수 있음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "prerequisite_id", nullable = false)
    private Long prerequisiteId;

    @Builder
    private CoursePrerequisite(Course course, Long prerequisiteId) {
        this.course = course;
        this.prerequisiteId = prerequisiteId;
    }
}

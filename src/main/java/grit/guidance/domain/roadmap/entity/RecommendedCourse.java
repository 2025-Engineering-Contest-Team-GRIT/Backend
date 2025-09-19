package grit.guidance.domain.roadmap.entity;

import grit.guidance.domain.course.entity.Course;
import grit.guidance.domain.course.entity.Semester;
import grit.guidance.domain.user.entity.Users;
import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "recommended_course")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE recommended_course SET updated_at = NOW(), deleted_at = NOW() WHERE recommended_course_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class RecommendedCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommended_course_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "recommended_description", columnDefinition = "TEXT")
    private String recommendDescription; // 추천 이유

    @Column(name = "recommend_grade", nullable = false)
    private Integer recommendGrade; // 추천 학년 (예: 3학년)

    @Enumerated(EnumType.STRING)
    @Column(name = "recommend_semester", nullable = false)
    private Semester recommendSemester; // 추천 학기
}
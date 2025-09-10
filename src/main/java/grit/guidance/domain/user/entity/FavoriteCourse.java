package grit.guidance.domain.user.entity;

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
@Table(name = "favorite_course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE favorite_course SET updated_at = NOW(), deleted_at = NOW() WHERE favorite_course_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class FavoriteCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "favorite_course_id")
    private Long id;

    // N:1 관계 - favorite_course와 users (양방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    // N:1 관계 - favorite_course와 course (단방향, course 도메인 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Builder
    public FavoriteCourse(Users users, Course course) {
        this.users = users;
        this.course = course;
    }

    // 비즈니스 메서드
    public boolean isSameUser(Users user) {
        return this.users.getId().equals(user.getId());
    }

    public boolean isSameCourse(Course course) {
        return this.course.getId().equals(course.getId());
    }
}

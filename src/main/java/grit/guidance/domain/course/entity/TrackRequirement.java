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
@Table(name = "track_requirement")
@Getter
//JPA는 엔티티 클래스에 파라미터 없는 기본 생성자가 반드시 존재해야 한다. JPA 구현체(주로 Hibernate)가 사용하는 '프록시(Proxy)' 기술과 관련이 깊기 때문. 나중에 더 공부할 내용.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE track_requirement SET updated_at = NOW(), deleted_at = NOW() WHERE track_requirement_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class TrackRequirement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_requirement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // (N:1 관계)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY) // (N:1 관계)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Enumerated(EnumType.STRING) // Enum타입이기 때문에 명시적 지정
    @Column(name = "course_type", nullable = false)
    private CourseType courseType;

    // 생성자를 private으로 막고 Builder방식으로만 생성 가능
    @Builder
    private TrackRequirement(Course course, Track track, CourseType courseType) {
        this.course = course;
        this.track = track;
        this.courseType = courseType;
    }
}
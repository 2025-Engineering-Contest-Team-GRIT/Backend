package grit.guidance.domain.course.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "hibernate_proxy"})
// 삭제 요청이 발생했을 때, 실제 DELETE 쿼리 대신 지정된 UPDATE 쿼리를 실행하도록 JPA에게 지시
@SQLDelete(sql = "UPDATE course SET updated_at = NOW(), deleted_at = NOW() WHERE course_id = ?")
// 모든 SELECT 쿼리에 삭제되지 않은 데이터만 찾아줌.
@SQLRestriction("deleted_at IS NULL")
public class Course extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(name = "course_name", nullable = false, length = 30)
    private String courseName;

    @Column(name = "course_code", nullable = false, length = 20)
    private String courseCode;

    @Column(name = "credits", nullable = false)
    private Integer credits;

    @Column(name = "course_description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "open_grade", nullable = false)
    private Integer openGrade;

    @Enumerated(EnumType.STRING) // Enum타입이기 때문에 명시적 지정
    @Column(name = "open_semester", nullable = false)
    private Semester openSemester;



    // 1:N 관계 - course와 completed_course (양방향)
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<grit.guidance.domain.user.entity.CompletedCourse> completedCourses = new ArrayList<>();

    // 1:N 관계 - course와 favorite_course (양방향)
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<grit.guidance.domain.user.entity.FavoriteCourse> favoriteCourses = new ArrayList<>();

    @Builder
    private Course(String courseName, String courseCode, Integer credits, String description,
                   Integer openGrade, Semester openSemester) {
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.credits = credits;
        this.description = description;
        this.openGrade = openGrade;
        this.openSemester = openSemester;
    }

    // 과목 설명 업데이트
    public void updateDescription(String description) {
        this.description = description;
    }

}
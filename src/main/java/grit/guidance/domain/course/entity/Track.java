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
@Table(name = "track")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE track SET updated_at = NOW(), deleted_at = NOW() WHERE track_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Track extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "track_id")
    private Long id;

    @Column(name = "track_name", nullable = false, length = 20)
    private String trackName;

    @Builder
    public Track(String trackName) {
        this.trackName = trackName;
    }
}
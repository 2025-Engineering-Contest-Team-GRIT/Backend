package grit.guidance.domain.user.entity;

import grit.guidance.domain.course.entity.Track;
import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_track")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE user_track SET updated_at = NOW(), deleted_at = NOW() WHERE user_track_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class UserTrack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_track_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users; // User 엔티티와의 연관관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track; // Track 엔티티와의 연관관계

    @Enumerated(EnumType.STRING)
    @Column(name = "track_type", nullable = false)
    private TrackType trackType;

    @Builder
    private UserTrack(Users users, Track track, TrackType trackType) {
        this.users = users;
        this.track = track;
        this.trackType = trackType;
    }
}
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
import java.time.LocalDateTime;

@Entity
@Table(name = "crawling_graduation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE crawling_graduation SET updated_at = NOW(), deleted_at = NOW() WHERE crawling_graduation_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CrawlingGraduation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crawling_graduation_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users users;

    @Column(name = "total_completed_credits")
    private Integer totalCompletedCredits;

    @Column(name = "track1_major_basic")
    private Integer track1MajorBasic;

    @Column(name = "track1_major_required")
    private Integer track1MajorRequired;

    @Column(name = "track1_major_subtotal")
    private Integer track1MajorSubtotal;

    @Column(name = "track2_major_basic")
    private Integer track2MajorBasic;

    @Column(name = "track2_major_required")
    private Integer track2MajorRequired;

    @Column(name = "track2_major_subtotal")
    private Integer track2MajorSubtotal;

    @Column(name = "total_major_completed")
    private Integer totalMajorCompleted;

    @Column(name = "total_major_required")
    private Integer totalMajorRequired;

    @Column(name = "last_crawled_time")
    private LocalDateTime lastCrawledTime;

    @Builder
    public CrawlingGraduation(Users users, Integer totalCompletedCredits, Integer track1MajorBasic, Integer track1MajorRequired, Integer track1MajorSubtotal, Integer track2MajorBasic, Integer track2MajorRequired, Integer track2MajorSubtotal, Integer totalMajorCompleted, Integer totalMajorRequired) {
        this.users = users;
        this.totalCompletedCredits = totalCompletedCredits;
        this.track1MajorBasic = track1MajorBasic;
        this.track1MajorRequired = track1MajorRequired;
        this.track1MajorSubtotal = track1MajorSubtotal;
        this.track2MajorBasic = track2MajorBasic;
        this.track2MajorRequired = track2MajorRequired;
        this.track2MajorSubtotal = track2MajorSubtotal;
        this.totalMajorCompleted = totalMajorCompleted;
        this.totalMajorRequired = totalMajorRequired;
        this.lastCrawledTime = LocalDateTime.now();
    }

    public void updateCrawlingData(Integer totalCompletedCredits, Integer track1MajorBasic, Integer track1MajorRequired, Integer track1MajorSubtotal, Integer track2MajorBasic, Integer track2MajorRequired, Integer track2MajorSubtotal, Integer totalMajorCompleted, Integer totalMajorRequired) {
        this.totalCompletedCredits = totalCompletedCredits;
        this.track1MajorBasic = track1MajorBasic;
        this.track1MajorRequired = track1MajorRequired;
        this.track1MajorSubtotal = track1MajorSubtotal;
        this.track2MajorBasic = track2MajorBasic;
        this.track2MajorRequired = track2MajorRequired;
        this.track2MajorSubtotal = track2MajorSubtotal;
        this.totalMajorCompleted = totalMajorCompleted;
        this.totalMajorRequired = totalMajorRequired;
        this.lastCrawledTime = LocalDateTime.now();
    }
}
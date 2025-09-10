package grit.guidance.domain.user.entity;

import grit.guidance.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "graduation_requirement")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE graduation_requirement SET updated_at = NOW(), deleted_at = NOW() WHERE graduation_requirement_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GraduationRequirement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "graduation_requirement_id")
    private Long id;

    // 1:1 관계 - graduation_requirement와 users (양방향)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users users;

    @Column(name = "capstone_completed", nullable = false)
    private Boolean capstoneCompleted = false; // 캡스톤 출품 여부

    @Column(name = "thesis_submitted", nullable = false)
    private Boolean thesisSubmitted = false; // 졸업 논문 제출 여부

    @Column(name = "award_or_certificate_received", nullable = false)
    private Boolean awardOrCertificateReceived = false; // 자격증/입상 여부

    @Builder
    public GraduationRequirement(Users users, Boolean capstoneCompleted, 
                               Boolean thesisSubmitted, Boolean awardOrCertificateReceived) {
        this.users = users;
        this.capstoneCompleted = capstoneCompleted != null ? capstoneCompleted : false;
        this.thesisSubmitted = thesisSubmitted != null ? thesisSubmitted : false;
        this.awardOrCertificateReceived = awardOrCertificateReceived != null ? awardOrCertificateReceived : false;
    }

    // 비즈니스 메서드
    public void updateCapstoneStatus(Boolean completed) {
        this.capstoneCompleted = completed;
    }

    public void updateThesisStatus(Boolean submitted) {
        this.thesisSubmitted = submitted;
    }

    public void updateAwardStatus(Boolean received) {
        this.awardOrCertificateReceived = received;
    }

    public boolean isAllRequirementsMet() {
        return capstoneCompleted && thesisSubmitted && awardOrCertificateReceived;
    }
}

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
@Table(name = "setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE setting SET updated_at = NOW(), deleted_at = NOW() WHERE setting_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Setting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    // 1:1 관계 - setting과 users (양방향)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private Users users;

    @Column(name = "setting_info", columnDefinition = "TEXT")
    private String settingInfo; // JSON 형태의 설정 정보 (null이면 기본 설정 적용)

    @Builder
    public Setting(Users users, String settingInfo) {
        this.users = users;
        this.settingInfo = settingInfo;
    }

    // 비즈니스 메서드
    public void updateSettingInfo(String settingInfo) {
        this.settingInfo = settingInfo;
    }

    public void resetToDefault() {
        this.settingInfo = null; // null이면 기본 설정 JSON 적용
    }
}


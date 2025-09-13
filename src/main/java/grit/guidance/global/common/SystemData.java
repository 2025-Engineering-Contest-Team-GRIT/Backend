package grit.guidance.global.common;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemData extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "global_update_time", nullable = false)
    @Builder.Default
    private LocalDateTime globalUpdateTime = LocalDateTime.of(2000, 1, 1, 0, 0); // 기본 값 2000년 1월 1일 0시 0분
}

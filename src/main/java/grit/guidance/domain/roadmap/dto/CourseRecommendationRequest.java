package grit.guidance.domain.roadmap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRecommendationRequest {
    @JsonProperty("student_id")
    private String studentId;  // 학번 (예: "2191232")
    
    @JsonProperty("track_ids")
    private List<Long> trackIds;  // [1, 2] - 모바일소프트웨어, 웹공학
    
    @JsonProperty("learning_style")
    private LearningStyle learningStyle;
    
    @JsonProperty("advanced_settings")
    private AdvancedSettings advancedSettings;  // Optional

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningStyle {
        @JsonProperty("credits_per_semester")
        private String creditsPerSemester;  // "RELAXED", "NORMAL", "INTENSIVE"
        
        @JsonProperty("style_preference")
        private String stylePreference;     // "THEORY", "BALANCED", "PRACTICE"
        
        @JsonProperty("ratio_preference")
        private String ratioPreference;     // "MAJOR", "BALANCED", "LIBERAL_ARTS"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvancedSettings {
        @JsonProperty("tech_stack")
        private String techStack;  // "React, Python, AWS"
    }
}
